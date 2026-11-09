"""Teste de reservas simultaneas contra a API rodando (verificacao da V6).

Dispara N requisicoes POST /api/appointments para o MESMO horario, todas
liberadas no mesmo instante por uma threading.Barrier, e conta os status
devolvidos. Com o indice uq_appointments_horario_ativo (V6), cada rodada deve
ter exatamente um 201; as demais recebem 409 (o indice recusou) ou 400 (o
validador ja viu a reserva gravada).

So usa a biblioteca padrao do Python. Precisa da Auth API (8081) e da Core API
(8080) rodando contra o Postgres: os testes automatizados usam H2, que nao
aceita indice parcial, entao este e o unico jeito de exercitar a V6.

Uso:
    python scripts/teste-concorrencia-v6.py 2026-11-20
    python scripts/teste-concorrencia-v6.py 2026-11-20 --servico "Corte Social"

A data precisa ter horario de funcionamento cadastrado das 10:00 as 17:00 e
estar livre nesses horarios. O script cadastra 10 clientes de teste
(teste.concorrencia0..9@exemplo.com) e imprime os ids dos agendamentos
criados, para que possam ser apagados depois.
"""
import argparse
import collections
import json
import threading
import urllib.error
import urllib.request

AUTH = "http://localhost:8081/api/auth"
CORE = "http://localhost:8080/api"


def req(method, url, body=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(url, method=method, data=data, headers=headers)
    try:
        with urllib.request.urlopen(r) as resp:
            return resp.status, json.loads(resp.read() or b"null")
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read() or b"null")


def token(i):
    email = f"teste.concorrencia{i}@exemplo.com"
    # Se o cliente ja existir, o cadastro falha e o login segue normalmente.
    req("POST", AUTH + "/register", {"username": f"Cliente Teste {i}", "email": email,
                                     "password": "senha123", "phone": f"449900000{i:02d}"})
    status, corpo = req("POST", AUTH + "/login", {"email": email, "password": "senha123"})
    if status != 200:
        raise SystemExit(f"login de {email} falhou: {status} {corpo}")
    return corpo["accessToken"]


def rodada(servico, tokens, horario, n, criados):
    barreira = threading.Barrier(n)
    res = [None] * n

    def vai(i):
        barreira.wait()
        res[i] = req("POST", CORE + "/appointments",
                     {"serviceId": servico, "dateTime": horario}, tokens[i])

    threads = [threading.Thread(target=vai, args=(i,)) for i in range(n)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()

    criados += [corpo["id"] for status, corpo in res if status == 201]
    contagem = collections.Counter(status for status, _ in res)
    resumo = ", ".join(f"{k} x{v}" for k, v in sorted(contagem.items()))
    print(f"{n} requisicoes simultaneas para {horario}: {resumo}")
    return contagem[201] == 1


def main():
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    p.add_argument("data", help="dia das reservas, AAAA-MM-DD")
    p.add_argument("--servico", default="Degrade", help="nome do servico (padrao: Degrade)")
    args = p.parse_args()

    servicos = [s for s in req("GET", CORE + "/services")[1] if s["serviceName"] == args.servico]
    if not servicos:
        raise SystemExit(f"servico '{args.servico}' nao encontrado")
    servico = servicos[0]["id"]
    tokens = [token(i) for i in range(10)]

    criados = []
    ok = True
    for hora in ["10:00", "11:00", "14:00"]:
        ok &= rodada(servico, tokens, f"{args.data}T{hora}:00", 2, criados)
    for hora in ["15:00", "16:00"]:
        ok &= rodada(servico, tokens, f"{args.data}T{hora}:00", 10, criados)

    print("agendamentos criados (apagar depois):", json.dumps(criados))
    print("RESULTADO:", "exatamente um 201 por horario" if ok else "FALHOU: algum horario aceitou mais de uma reserva")
    raise SystemExit(0 if ok else 1)


if __name__ == "__main__":
    main()
