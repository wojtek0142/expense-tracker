import io
import sqlite3
import uuid
from contextlib import asynccontextmanager
from datetime import date, timedelta
from pathlib import Path
from typing import Optional

from fastapi import Depends, FastAPI, File, HTTPException, Query, UploadFile
from PIL import Image
from pydantic import BaseModel, Field

from database import find_or_create_product, get_conn, get_default_category, init_db

UPLOAD_DIR = Path(__file__).parent / "uploads"
UPLOAD_DIR.mkdir(exist_ok=True)
MAX_UPLOAD_BYTES = 10 * 1024 * 1024  # 10 MB


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    yield


app = FastAPI(title="Expense Tracker API", lifespan=lifespan)


def get_db():
    """Otwiera połączenie z bazą na czas jednego żądania i zawsze je zamyka."""
    conn = get_conn()
    try:
        yield conn
    finally:
        conn.close()


def month_range(month: str):
    """'2026-10' -> ('2026-10-01', '2026-10-31')"""
    try:
        y, m = map(int, month.split("-"))
        first = date(y, m, 1)
    except ValueError:
        raise HTTPException(400, "month musi mieć format YYYY-MM")
    nxt = date(y + (m == 12), m % 12 + 1, 1)
    return first.isoformat(), (nxt - timedelta(days=1)).isoformat()


def prev_month(month: str) -> str:
    y, m = map(int, month.split("-"))
    return f"{y - 1}-12" if m == 1 else f"{y}-{m - 1:02d}"


def current_month() -> str:
    return date.today().strftime("%Y-%m")


def resolve_range(month: Optional[str], date_from: Optional[date], date_to: Optional[date]):
    """Zwraca (od, do) jako teksty. Podaj ALBO oba date_from+date_to, ALBO month."""
    if (date_from is None) != (date_to is None):
        raise HTTPException(400, "Podaj jednocześnie date_from i date_to")
    if date_from and date_to:
        if date_from > date_to:
            raise HTTPException(400, "date_from nie może być późniejsze niż date_to")
        return date_from.isoformat(), date_to.isoformat()
    return month_range(month or current_month())


def total_between(conn, d_from: str, d_to: str) -> float:
    row = conn.execute(
        "SELECT COALESCE(SUM(amount), 0) AS t FROM expenses WHERE date BETWEEN ? AND ?",
        (d_from, d_to),
    ).fetchone()
    return round(row["t"], 2)


def category_exists(conn, category_id: int) -> bool:
    return conn.execute("SELECT 1 FROM categories WHERE id = ?", (category_id,)).fetchone() is not None


def is_valid_image(data: bytes) -> bool:
    """Sprawdza, czy bajty to naprawdę obraz."""
    try:
        Image.open(io.BytesIO(data)).verify()
        return True
    except Exception:
        return False


EXPENSE_SELECT = """
    SELECT e.id, e.product_id, p.name AS product, e.quantity,
           ROUND(e.amount / e.quantity, 2) AS price,   -- cena za 1 szt.
           e.amount,                                   -- cena łącznie (ilość x cena)
           e.date, p.category_id, c.name AS category, e.receipt_id
    FROM expenses e
    JOIN products   p ON p.id = e.product_id
    JOIN categories c ON c.id = p.category_id
"""


# ---------- modele danych wysyłanych przez aplikację ----------
class ExpenseIn(BaseModel):
    product_name: str                   # np. "Mleko 3,2%"
    quantity: float = Field(1, gt=0)    # ilość (domyślnie 1)
    price: float = Field(gt=0)          # cena ZA 1 SZTUKĘ
    date: date                          # "2026-10-11"
    category_id: Optional[int] = None   # tylko dla nowego produktu (inaczej 'default')
    receipt_id: Optional[int] = None    # opcjonalnie: powiązanie ze zdjęciem paragonu


class CategoryIn(BaseModel):
    name: str


class ProductCategoryIn(BaseModel):
    category_id: int


class BudgetIn(BaseModel):
    amount: float = Field(gt=0)


# ---------- kategorie ----------
@app.get("/categories")
def list_categories(conn: sqlite3.Connection = Depends(get_db)):
    rows = conn.execute("SELECT * FROM categories ORDER BY id").fetchall()
    return [dict(r) for r in rows]


@app.post("/categories", status_code=201)
def create_category(cat: CategoryIn, conn: sqlite3.Connection = Depends(get_db)):
    name = cat.name.strip()
    if not name:
        raise HTTPException(400, "Nazwa kategorii nie może być pusta")
    try:
        cur = conn.execute("INSERT INTO categories (name) VALUES (?)", (name,))
        conn.commit()
    except sqlite3.IntegrityError:
        raise HTTPException(409, "Kategoria o takiej nazwie już istnieje")
    return {"id": cur.lastrowid, "name": name, "is_builtin": 0}


# ---------- produkty (ekran przypisywania kategorii) ----------
@app.get("/products")
def list_products(
    category_id: Optional[int] = None,
    only_default: bool = False,
    conn: sqlite3.Connection = Depends(get_db),
):
    """Lista produktów. only_default=true -> tylko te z kategorią 'default' (jeszcze nieprzypisane)."""
    sql = """SELECT p.id, p.name, p.category_id, c.name AS category,
                    (SELECT COUNT(*) FROM expenses e WHERE e.product_id = p.id) AS purchases
             FROM products p JOIN categories c ON c.id = p.category_id WHERE 1 = 1"""
    params = []
    if only_default:
        sql += " AND p.category_id = ?"
        params.append(get_default_category(conn)["id"])
    elif category_id:
        sql += " AND p.category_id = ?"
        params.append(category_id)
    sql += " ORDER BY p.name"
    return [dict(r) for r in conn.execute(sql, params).fetchall()]


@app.put("/products/{product_id}/category")
def set_product_category(product_id: int, body: ProductCategoryIn,
                         conn: sqlite3.Connection = Depends(get_db)):
    """Przypisz kategorię produktowi. Dotyczy też wszystkich jego wcześniejszych zakupów."""
    if not category_exists(conn, body.category_id):
        raise HTTPException(404, "Nie ma kategorii o takim id")
    cur = conn.execute("UPDATE products SET category_id = ? WHERE id = ?", (body.category_id, product_id))
    if cur.rowcount == 0:
        raise HTTPException(404, "Nie ma produktu o takim id")
    conn.commit()
    return {"product_id": product_id, "category_id": body.category_id}


# ---------- wydatki ----------
@app.post("/expenses", status_code=201)
def add_expense(e: ExpenseIn, conn: sqlite3.Connection = Depends(get_db)):
    """Dodanie wydatku. Znany produkt -> jego kategoria. Nowy produkt -> trafia do słownika
    (z kategorią 'default' albo z podanym category_id)."""
    name = e.product_name.strip()
    if not name:
        raise HTTPException(400, "Nazwa produktu nie może być pusta")
    if e.category_id is not None and not category_exists(conn, e.category_id):
        raise HTTPException(404, "Nie ma kategorii o takim id")
    if e.receipt_id is not None and not conn.execute(
            "SELECT 1 FROM receipts WHERE id = ?", (e.receipt_id,)).fetchone():
        raise HTTPException(404, "Nie ma paragonu o takim id")

    product_id = find_or_create_product(conn, name, e.category_id)
    amount = round(e.quantity * e.price, 2)
    cur = conn.execute(
        "INSERT INTO expenses (product_id, quantity, amount, date, receipt_id) VALUES (?, ?, ?, ?, ?)",
        (product_id, e.quantity, amount, e.date.isoformat(), e.receipt_id),
    )
    conn.commit()
    return {"id": cur.lastrowid, "product_id": product_id, "amount": amount}


@app.get("/expenses")
def get_expenses(
    date_from: Optional[date] = None,
    date_to: Optional[date] = None,
    category_id: Optional[int] = None,
    limit: int = Query(100, le=1000),
    conn: sqlite3.Connection = Depends(get_db),
):
    """Wydatki po zakresie dat i/lub kategorii (najnowsze pierwsze)."""
    if date_from and date_to and date_from > date_to:
        raise HTTPException(400, "date_from nie może być późniejsze niż date_to")
    sql = EXPENSE_SELECT + " WHERE 1 = 1"
    params = []
    if date_from:
        sql += " AND e.date >= ?"
        params.append(date_from.isoformat())
    if date_to:
        sql += " AND e.date <= ?"
        params.append(date_to.isoformat())
    if category_id:
        sql += " AND p.category_id = ?"
        params.append(category_id)
    sql += " ORDER BY e.date DESC, e.id DESC LIMIT ?"
    params.append(limit)
    return [dict(r) for r in conn.execute(sql, params).fetchall()]


@app.get("/expenses/recent")
def recent_expenses(limit: int = Query(10, le=100), conn: sqlite3.Connection = Depends(get_db)):
    """Ekran główny: 'Ostatnie transakcje'."""
    return get_expenses(limit=limit, conn=conn)


@app.delete("/expenses/{expense_id}", status_code=204)
def delete_expense(expense_id: int, conn: sqlite3.Connection = Depends(get_db)):
    conn.execute("DELETE FROM expenses WHERE id = ?", (expense_id,))
    conn.commit()


# ---------- podsumowania (ekran raportów) ----------
@app.get("/summary/categories")
def summary_by_category(
    month: Optional[str] = None,
    date_from: Optional[date] = None,
    date_to: Optional[date] = None,
    conn: sqlite3.Connection = Depends(get_db),
):
    """Suma w każdej kategorii. Podaj month=YYYY-MM LUB date_from+date_to (filtr zakresu).
    Dane do wykresu kołowego i 'top 3' (pierwsze 3 elementy listy)."""
    d_from, d_to = resolve_range(month, date_from, date_to)
    rows = conn.execute(
        """SELECT c.id AS category_id, c.name AS category, ROUND(SUM(e.amount), 2) AS total
           FROM expenses e
           JOIN products   p ON p.id = e.product_id
           JOIN categories c ON c.id = p.category_id
           WHERE e.date BETWEEN ? AND ?
           GROUP BY c.id ORDER BY total DESC""",
        (d_from, d_to),
    ).fetchall()
    items = [dict(r) for r in rows]
    total = round(sum(i["total"] for i in items), 2)
    for i in items:
        i["percent"] = round(i["total"] / total * 100, 1) if total else 0
    return {"date_from": d_from, "date_to": d_to, "total": total,
            "categories": items, "top3": items[:3]}


@app.get("/summary/compare")
def compare_months(month: Optional[str] = None, conn: sqlite3.Connection = Depends(get_db)):
    """'Suma wydatków w tym i poprzednim miesiącu'."""
    month = month or current_month()
    month_range(month)
    pm = prev_month(month)
    cur_total = total_between(conn, *month_range(month))
    prev_total = total_between(conn, *month_range(pm))
    diff = round(cur_total - prev_total, 2)
    pct = round(diff / prev_total * 100, 1) if prev_total else None
    return {"month": month, "total": cur_total, "previous_month": pm,
            "previous_total": prev_total, "difference": diff, "difference_percent": pct}


@app.get("/summary/averages")
def averages(month: Optional[str] = None, conn: sqlite3.Connection = Depends(get_db)):
    """Średnia dzienna i tygodniowa w miesiącu (liczona do dziś, jeśli to bieżący miesiąc)."""
    month = month or current_month()
    d_from, d_to = month_range(month)
    end = min(date.fromisoformat(d_to), date.today())
    days = max((end - date.fromisoformat(d_from)).days + 1, 1)
    total = total_between(conn, d_from, d_to)
    return {"month": month, "total": total, "days": days,
            "daily_avg": round(total / days, 2), "weekly_avg": round(total / days * 7, 2)}


# ---------- budżet (ekran główny) ----------
@app.put("/budget/{month}")
def set_budget(month: str, b: BudgetIn, conn: sqlite3.Connection = Depends(get_db)):
    month_range(month)
    conn.execute("INSERT OR REPLACE INTO budgets (month, amount) VALUES (?, ?)", (month, b.amount))
    conn.commit()
    return {"month": month, "amount": b.amount}


@app.get("/budget")
def get_budget(month: Optional[str] = None, conn: sqlite3.Connection = Depends(get_db)):
    """'Budżet w tym miesiącu': limit, wydano, pozostało."""
    month = month or current_month()
    spent = total_between(conn, *month_range(month))
    row = conn.execute("SELECT amount FROM budgets WHERE month = ?", (month,)).fetchone()
    limit = row["amount"] if row else None
    return {"month": month, "budget": limit, "spent": spent,
            "remaining": round(limit - spent, 2) if limit is not None else None}


# ---------- zdjęcia paragonów ----------
@app.post("/receipts", status_code=201)
def upload_receipt(file: UploadFile = File(...), conn: sqlite3.Connection = Depends(get_db)):
    """Aplikacja wysyła zdjęcie (multipart/form-data, pole 'file').
    Serwer sprawdza, że to obraz, zapisuje plik w uploads/ i dodaje wiersz do tabeli receipts."""
    data = file.file.read(MAX_UPLOAD_BYTES + 1)
    if len(data) > MAX_UPLOAD_BYTES:
        raise HTTPException(413, "Plik jest za duży (max 10 MB)")
    if not is_valid_image(data):
        raise HTTPException(400, "Plik nie jest poprawnym obrazem")

    filename = f"{uuid.uuid4().hex}{Path(file.filename or '').suffix or '.jpg'}"
    (UPLOAD_DIR / filename).write_bytes(data)
    cur = conn.execute("INSERT INTO receipts (image_path) VALUES (?)", (filename,))
    conn.commit()
    return {"id": cur.lastrowid, "image_path": filename}


@app.get("/receipts")
def list_receipts(conn: sqlite3.Connection = Depends(get_db)):
    return [dict(r) for r in conn.execute("SELECT * FROM receipts ORDER BY id DESC").fetchall()]


@app.get("/receipts/{receipt_id}")
def get_receipt(receipt_id: int, conn: sqlite3.Connection = Depends(get_db)):
    row = conn.execute("SELECT * FROM receipts WHERE id = ?", (receipt_id,)).fetchone()
    if not row:
        raise HTTPException(404, "Nie ma takiego paragonu")
    return dict(row)