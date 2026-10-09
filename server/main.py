import json
import sqlite3

from fastapi import FastAPI, HTTPException, UploadFile
from pydantic import BaseModel

app = FastAPI()
DB_FILE = "expenses.db"


def get_db():
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn


def create_tables():
    conn = get_db()
    conn.execute("""
        CREATE TABLE IF NOT EXISTS categories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE
        )
    """)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS products (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            price REAL,
            category_id INTEGER REFERENCES categories(id)
        )
    """)
    # domyślne kategorie
    for name in ["Jedzenie", "Paliwo", "Transport", "Rozrywka", "Zdrowie", "Rachunki"]:
        conn.execute("INSERT OR IGNORE INTO categories (name) VALUES (?)", (name,))
    conn.commit()
    conn.close()


create_tables()


# Kategorie

@app.get("/categories")
def list_categories():
    conn = get_db()
    rows = conn.execute("SELECT id, name FROM categories").fetchall()
    conn.close()
    return [dict(row) for row in rows]


# Produkty

@app.post("/products/import")
async def import_products(file: UploadFile):
    """Wysyłasz plik JSON z listą produktów. Trafiają do bazy bez kategorii."""
    products = json.loads(await file.read())
    conn = get_db()
    for p in products:
        conn.execute(
            "INSERT INTO products (name, price) VALUES (?, ?)",
            (p["name"], p.get("price")),
        )
    conn.commit()
    conn.close()
    return {"added": len(products)}


@app.get("/products")
def list_products():
    conn = get_db()
    rows = conn.execute("""
        SELECT products.id, products.name, products.price,
               products.category_id, categories.name AS category
        FROM products
        LEFT JOIN categories ON categories.id = products.category_id
    """).fetchall()
    conn.close()
    return [dict(row) for row in rows]


class CategoryChoice(BaseModel):
    category_id: int


@app.put("/products/{product_id}/category")
def set_category(product_id: int, choice: CategoryChoice):
    """Przypisuje kategorię do wybranego produktu."""
    conn = get_db()
    category = conn.execute(
        "SELECT id FROM categories WHERE id = ?", (choice.category_id,)
    ).fetchone()
    if category is None:
        conn.close()
        raise HTTPException(status_code=404, detail="Nie ma takiej kategorii")

    result = conn.execute(
        "UPDATE products SET category_id = ? WHERE id = ?",
        (choice.category_id, product_id),
    )
    conn.commit()
    conn.close()
    if result.rowcount == 0:
        raise HTTPException(status_code=404, detail="Nie ma takiego produktu")
    return {"product_id": product_id, "category_id": choice.category_id}