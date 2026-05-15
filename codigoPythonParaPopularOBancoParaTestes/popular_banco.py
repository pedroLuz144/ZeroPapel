"""
Script para popular o banco de dados ZeroPapel com dados de teste.

Instale as dependências antes de rodar:
    pip install pymysql bcrypt

Como rodar:
    python popular_banco.py
"""

import pymysql
import bcrypt
from datetime import datetime

# ---------------------------------------------------------------------------
# Configuração do banco — mesmos valores do seu .env
# ---------------------------------------------------------------------------

BANCO = {
    "host":     "localhost",
    "port":     3306,
    "db":       "zeropapel",
    "user":     "root",
    "password": "root",
    "charset":  "utf8mb4",
}

# ---------------------------------------------------------------------------
# Dados de teste
# ---------------------------------------------------------------------------

CATEGORIAS = [
    "Porcoes",
    "Petiscos",
    "Bebidas",
]

# (nome, categoria, preco, descricao)
ITENS = [
    ("Frango na Brasa",     "Porcoes",  45.00, "Frango grelhado inteiro"),
    ("Porcao de Calabresa", "Porcoes",  35.00, "500g de calabresa frita"),
    ("Camarao Empanado",    "Petiscos", 55.00, "300g de camarao empanado"),
    ("Batata Frita",        "Petiscos", 28.00, "Porcao de batata frita"),
    ("Cerveja 600ml",       "Bebidas",  12.00, None),
    ("Refrigerante",        "Bebidas",   7.00, None),
    ("Agua",                "Bebidas",   4.00, None),
]

# (nome, taxa_percentual)
PLATAFORMAS = [
    ("Balcao",   0.00),
    ("iFood",   12.00),
    ("AnotaAi", 10.00),
]

# (nome, taxa_percentual)
FORMAS_DE_PAGAMENTO = [
    ("Pix",     0.00),
    ("Credito", 2.99),
    ("Debito",  1.50),
]

# (nome_completo, usuario, senha_em_texto, cargo)
USUARIOS = [
    ("Carlos Gerente", "carlos", "senha123", "GERENTE"),
    ("Ana Operadora",  "ana",    "senha123", "OPERADOR"),
]

# Pedidos da noite de 13/05/2026 — use este intervalo no POST /fechamento:
#   {"de": "2026-05-13T18:00:00", "ate": "2026-05-14T00:00:00"}
#
# Cada pedido: (plataforma, forma_pagamento, nome_cliente_ou_None, horario, itens)
# Itens: lista de (nome_item, quantidade)
PEDIDOS = [
    ("Balcao",   "Pix",     None,       datetime(2026, 5, 13, 18, 10), [("Frango na Brasa", 1), ("Cerveja 600ml", 2)]),
    ("Balcao",   "Credito", None,       datetime(2026, 5, 13, 18, 25), [("Porcao de Calabresa", 1), ("Refrigerante", 2)]),
    ("iFood",    "Credito", "Marcos",   datetime(2026, 5, 13, 18, 40), [("Camarao Empanado", 1), ("Refrigerante", 1)]),
    ("Balcao",   "Pix",     None,       datetime(2026, 5, 13, 19,  5), [("Frango na Brasa", 2), ("Cerveja 600ml", 3)]),
    ("Balcao",   "Debito",  None,       datetime(2026, 5, 13, 19, 20), [("Batata Frita", 2), ("Agua", 2)]),
    ("iFood",    "Credito", "Fernanda", datetime(2026, 5, 13, 19, 35), [("Porcao de Calabresa", 1), ("Cerveja 600ml", 2)]),
    ("AnotaAi",  "Pix",     "Joao",     datetime(2026, 5, 13, 19, 50), [("Frango na Brasa", 1), ("Batata Frita", 1)]),
    ("Balcao",   "Pix",     None,       datetime(2026, 5, 13, 20,  5), [("Camarao Empanado", 2)]),
    ("Balcao",   "Credito", None,       datetime(2026, 5, 13, 20, 20), [("Frango na Brasa", 1), ("Refrigerante", 1), ("Cerveja 600ml", 1)]),
    ("iFood",    "Credito", "Patricia", datetime(2026, 5, 13, 20, 35), [("Camarao Empanado", 1), ("Batata Frita", 1), ("Refrigerante", 2)]),
    ("Balcao",   "Pix",     None,       datetime(2026, 5, 13, 20, 50), [("Porcao de Calabresa", 2), ("Cerveja 600ml", 4)]),
    ("AnotaAi",  "Pix",     "Roberto",  datetime(2026, 5, 13, 21,  5), [("Frango na Brasa", 1), ("Cerveja 600ml", 2)]),
    ("Balcao",   "Debito",  None,       datetime(2026, 5, 13, 21, 20), [("Batata Frita", 1), ("Refrigerante", 3)]),
    ("iFood",    "Credito", "Luciana",  datetime(2026, 5, 13, 21, 35), [("Frango na Brasa", 1), ("Porcao de Calabresa", 1)]),
    ("Balcao",   "Pix",     None,       datetime(2026, 5, 13, 21, 50), [("Camarao Empanado", 1), ("Cerveja 600ml", 2)]),
    ("Balcao",   "Credito", None,       datetime(2026, 5, 13, 22, 10), [("Frango na Brasa", 1), ("Agua", 2)]),
    ("iFood",    "Pix",     "Diego",    datetime(2026, 5, 13, 22, 30), [("Batata Frita", 2), ("Refrigerante", 2)]),
    ("Balcao",   "Debito",  None,       datetime(2026, 5, 13, 22, 45), [("Porcao de Calabresa", 1), ("Cerveja 600ml", 3)]),
    ("AnotaAi",  "Credito", "Camila",   datetime(2026, 5, 13, 23,  5), [("Camarao Empanado", 1), ("Frango na Brasa", 1)]),
    ("Balcao",   "Pix",     None,       datetime(2026, 5, 13, 23, 20), [("Frango na Brasa", 2), ("Cerveja 600ml", 2)]),
]

# ---------------------------------------------------------------------------
# Funcoes auxiliares
# ---------------------------------------------------------------------------

def gerar_hash_senha(senha_em_texto):
    senha_bytes = senha_em_texto.encode("utf-8")
    salt = bcrypt.gensalt()
    hash_bytes = bcrypt.hashpw(senha_bytes, salt)
    return hash_bytes.decode("utf-8")


def calcular_valor_pedido(itens_do_pedido, precos_dos_itens):
    total = 0.0
    for nome_item, quantidade in itens_do_pedido:
        total += precos_dos_itens[nome_item] * quantidade
    return total

# ---------------------------------------------------------------------------
# Popular o banco
# ---------------------------------------------------------------------------

def popular_banco():
    conexao = pymysql.connect(**BANCO)
    cursor = conexao.cursor()

    try:
        print("Limpando dados existentes...")
        cursor.execute("DELETE FROM fechamentos_caixa")
        cursor.execute("DELETE FROM itens_pedido")
        cursor.execute("DELETE FROM pedidos")
        cursor.execute("DELETE FROM refresh_tokens")
        cursor.execute("DELETE FROM usuarios")
        cursor.execute("DELETE FROM cardapio")
        cursor.execute("DELETE FROM categorias")
        cursor.execute("DELETE FROM plataforma")
        cursor.execute("DELETE FROM forma_de_pagamento")

        # --- Categorias ---
        print("Inserindo categorias...")
        ids_categorias = {}
        for nome_categoria in CATEGORIAS:
            cursor.execute("INSERT INTO categorias (nome) VALUES (%s)", (nome_categoria,))
            ids_categorias[nome_categoria] = cursor.lastrowid

        # --- Itens do cardapio ---
        print("Inserindo itens do cardapio...")
        precos_dos_itens = {}
        ids_dos_itens = {}
        for nome_item, nome_categoria, preco, descricao in ITENS:
            cursor.execute(
                "INSERT INTO cardapio (nome, categoria_id, preco, status, descricao) VALUES (%s, %s, %s, %s, %s)",
                (nome_item, ids_categorias[nome_categoria], preco, "ATIVO", descricao)
            )
            ids_dos_itens[nome_item] = cursor.lastrowid
            precos_dos_itens[nome_item] = preco

        # --- Plataformas ---
        print("Inserindo plataformas...")
        ids_plataformas = {}
        for nome_plataforma, taxa in PLATAFORMAS:
            cursor.execute(
                "INSERT INTO plataforma (nome, taxa_percentual) VALUES (%s, %s)",
                (nome_plataforma, taxa)
            )
            ids_plataformas[nome_plataforma] = cursor.lastrowid

        # --- Formas de pagamento ---
        print("Inserindo formas de pagamento...")
        ids_formas_de_pagamento = {}
        for nome_forma, taxa in FORMAS_DE_PAGAMENTO:
            cursor.execute(
                "INSERT INTO forma_de_pagamento (nome, taxa_percentual) VALUES (%s, %s)",
                (nome_forma, taxa)
            )
            ids_formas_de_pagamento[nome_forma] = cursor.lastrowid

        # --- Usuarios ---
        print("Inserindo usuarios (gerando hashes BCrypt, pode demorar alguns segundos)...")
        for nome_completo, usuario, senha_texto, cargo in USUARIOS:
            senha_hash = gerar_hash_senha(senha_texto)
            cursor.execute(
                "INSERT INTO usuarios (nome, usuario, senha, cargo) VALUES (%s, %s, %s, %s)",
                (nome_completo, usuario, senha_hash, cargo)
            )

        # --- Pedidos e itens de cada pedido ---
        print("Inserindo pedidos...")
        for plataforma, forma_pagamento, nome_cliente, horario, itens_do_pedido in PEDIDOS:
            valor_total = calcular_valor_pedido(itens_do_pedido, precos_dos_itens)

            cursor.execute(
                "INSERT INTO pedidos (plataforma_id, nome_cliente, horario_pedido, forma_de_pagamento_id, valor) VALUES (%s, %s, %s, %s, %s)",
                (
                    ids_plataformas[plataforma],
                    nome_cliente,
                    horario,
                    ids_formas_de_pagamento[forma_pagamento],
                    valor_total,
                )
            )
            id_do_pedido = cursor.lastrowid

            for nome_item, quantidade in itens_do_pedido:
                cursor.execute(
                    "INSERT INTO itens_pedido (pedido_id, item_id, quantidade, preco_unitario) VALUES (%s, %s, %s, %s)",
                    (id_do_pedido, ids_dos_itens[nome_item], quantidade, precos_dos_itens[nome_item])
                )

        conexao.commit()

        print("")
        print("Banco populado com sucesso! 20 pedidos inseridos.")
        print("")
        print("Credenciais de acesso:")
        print("  Gerente  -> usuario: carlos | senha: senha123")
        print("  Operador -> usuario: ana    | senha: senha123")
        print("")
        print("Para testar o fechamento, faca login com o gerente e depois:")
        print("  POST /fechamento")
        print('  Body: {"de": "2026-05-13T18:00:00", "ate": "2026-05-14T00:00:00"}')

    except Exception as erro:
        conexao.rollback()
        print(f"Erro ao popular o banco: {erro}")
        raise

    finally:
        cursor.close()
        conexao.close()


if __name__ == "__main__":
    popular_banco()
