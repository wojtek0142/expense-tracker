"""Baza SQLite - zwykły moduł sqlite3 (bez ORM), żeby było prosto."""
import sqlite3
from pathlib import Path

DB_PATH = Path(__file__).parent / "expenses.db"

# Kategoria, którą dostaje każdy nowy produkt, dopóki użytkownik jej nie zmieni.
DEFAULT_CATEGORY = "default"
BUILTIN_CATEGORIES = [DEFAULT_CATEGORY, "Jedzenie", "Paliwo", "Transport", "Rozrywka", "Zdrowie", "Rachunki"]


def get_conn():
    # check_same_thread=False: FastAPI obsługuje żądania w różnych wątkach.
    # timeout: jeśli baza jest chwilowo zajęta, czekaj do 10 s zamiast rzucać błąd.
    conn = sqlite3.connect(DB_PATH, check_same_thread=False, timeout=10)
    conn.row_factory = sqlite3.Row  # wiersze jako słowniki
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def init_db():
    conn = get_conn()
    conn.execute("PRAGMA journal_mode = WAL")  # lepsza obsługa równoległych odczytów/zapisów
    conn.executescript(
        """
        CREATE TABLE IF NOT EXISTS categories (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            name       TEXT NOT NULL UNIQUE,
            is_builtin INTEGER NOT NULL DEFAULT 0      -- 1 = kategoria wbudowana, 0 = własna
        );

        -- Słownik produktów: jedna nazwa = jeden wiersz, z przypisaną kategorią.
        CREATE TABLE IF NOT EXISTS products (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            name        TEXT NOT NULL,                 -- nazwa do wyświetlania, np. "Mleko 3.2%"
            name_key    TEXT NOT NULL UNIQUE,          -- nazwa znormalizowana, służy do rozpoznawania
            category_id INTEGER NOT NULL REFERENCES categories(id)
        );

        -- Zdjęcia paragonów (na razie tylko zapis zdjęcia, bez odczytu tekstu).
        CREATE TABLE IF NOT EXISTS receipts (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            image_path TEXT NOT NULL,                  -- nazwa pliku w folderze uploads/
            created_at TEXT NOT NULL DEFAULT (datetime('now'))
        );

        -- Jedna pozycja kupiona = jeden wiersz. Kategorię bierzemy z produktu.
        CREATE TABLE IF NOT EXISTS expenses (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            product_id INTEGER NOT NULL REFERENCES products(id),
            quantity   REAL NOT NULL DEFAULT 1,
            amount     REAL NOT NULL,                  -- łączna cena pozycji
            date       TEXT NOT NULL,                  -- format YYYY-MM-DD
            receipt_id INTEGER REFERENCES receipts(id) -- NULL dla wydatków dodanych ręcznie
        );

        CREATE TABLE IF NOT EXISTS budgets (
            month  TEXT PRIMARY KEY,                   -- format YYYY-MM
            amount REAL NOT NULL
        );

        CREATE INDEX IF NOT EXISTS idx_expenses_date    ON expenses(date);
        CREATE INDEX IF NOT EXISTS idx_expenses_product ON expenses(product_id);
        CREATE INDEX IF NOT EXISTS idx_products_cat     ON products(category_id);
        """
    )
    for name in BUILTIN_CATEGORIES:
        conn.execute("INSERT OR IGNORE INTO categories (name, is_builtin) VALUES (?, 1)", (name,))
    conn.commit()
    conn.close()


# ---------- funkcje pomocnicze do produktów ----------
def normalize_name(name: str) -> str:
    """'  MLEKO  3,2% ' i 'mleko 3.2%' mają dać ten sam klucz."""
    return " ".join(name.lower().replace(",", ".").split())


def get_default_category(conn) -> sqlite3.Row:
    return conn.execute("SELECT id, name FROM categories WHERE name = ?", (DEFAULT_CATEGORY,)).fetchone()


def find_product(conn, name: str):
    """Zwraca wiersz produktu (z nazwą kategorii) albo None, jeśli takiego produktu jeszcze nie ma."""
    return conn.execute(
        """SELECT p.id, p.name, p.category_id, c.name AS category
           FROM products p JOIN categories c ON c.id = p.category_id
           WHERE p.name_key = ?""",
        (normalize_name(name),),
    ).fetchone()


def find_or_create_product(conn, name: str, category_id: int | None = None) -> int:
    """Znany produkt -> zwraca jego id (z jego kategorią). Nowy -> dodaje z kategorią 'default'."""
    existing = find_product(conn, name)
    if existing:
        return existing["id"]
    # INSERT OR IGNORE: gdyby dwa żądania naraz dodały ten sam produkt, drugie nie wywoła błędu
    conn.execute(
        "INSERT OR IGNORE INTO products (name, name_key, category_id) VALUES (?, ?, ?)",
        (name.strip(), normalize_name(name), category_id or get_default_category(conn)["id"]),
    )
    return find_product(conn, name)["id"]