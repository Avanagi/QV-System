import random
from locust import HttpUser, task, between
from locust.exception import StopUser

# === НАСТРОЙКИ ===
# Создай ПУБЛИЧНЫЙ КВАДРАТИЧНЫЙ опрос через UI. Посмотри его ID.
POLL_ID = 1
OPTION_RADICAL = 1  # Вариант, за который бьются "Богатые Радикалы" (например: "Снести парк")
OPTION_MODERATE = 2 # Вариант, который выгоден обществу (например: "Оставить парк")

class SociologicalVoter(HttpUser):
    host = "http://localhost:8080"
    wait_time = between(1, 3)
    user_id = None
    role = ""

    def on_start(self):
        # 1. Генерируем случайного человека и регистрируем его
        tg_id = random.randint(1000000, 9999999)
        username = f"Bot_{tg_id}"

        with self.client.post(f"/api/wallet/login/tg/{tg_id}?username={username}", catch_response=True) as response:
            if response.status_code == 200:
                data = response.json()
                self.user_id = data['user']['userId']
                # Берем токен для авторизации
                self.client.headers.update({"Authorization": f"Bearer {data['token']}"})
                response.success()
            else:
                response.failure(f"Ошибка регистрации")
                raise StopUser()

        # 2. Определяем долю в обществе (Социологическая модель)
        rand = random.random()
        if rand < 0.10:
            self.role = "rich_radical"   # 10% - Богатые и злые (Хотят снести парк)
        elif rand < 0.40:
            self.role = "rational"       # 30% - Средний класс (Хотят оставить парк, но распределяют бюджет)
        else:
            self.role = "lazy"           # 60% - Ленивое большинство (Слегка за парк, но тратить деньги жалко)

    @task
    def vote(self):
        if not self.user_id: raise StopUser()

        votes = {}

        # Распределение баланса (у всех есть 1000 QV)
        if self.role == "rich_radical":
            # Тратит почти все свои 1000 кредитов на ОДИН вариант (Радикал)
            # 31 голос = 961 кредит (максимум)
            votes[str(OPTION_RADICAL)] = 31

        elif self.role == "rational":
            # Распределяет силы рационально (Вектор мотивации)
            # 20 голосов = 400 кредитов (оставляет запас на другие опросы)
            votes[str(OPTION_MODERATE)] = 20

        elif self.role == "lazy":
            # Ленивый. Тратит копейки (1 голос = 1 кредит), просто чтобы отметиться
            votes[str(OPTION_MODERATE)] = 1

        payload = {
            "userId": self.user_id,
            "pollId": POLL_ID,
            "votes": votes
        }

        with self.client.post("/api/votes/batch", json=payload, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Vote failed: {response.text}")

        # Бот проголосовал и уходит из симуляции
        raise StopUser()