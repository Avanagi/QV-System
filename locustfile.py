import random
from locust import HttpUser, task, between
from locust.exception import StopUser

POLL_ID = 1
OPTION_A = 1
OPTION_B = 2

class QVUser(HttpUser):
    host = "http://localhost:8080"

    wait_time = between(1, 3)

    user_id = None
    role = "moderate"

    def on_start(self):
        """Выполняется при создании каждого бота"""
        tg_id = random.randint(1000000, 9999999)

        with self.client.post(f"/api/auth/login/{tg_id}", catch_response=True) as response:
            if response.status_code == 200:
                data = response.json()
                self.user_id = data['user']['userId']
                token = data['token']

                self.client.headers.update({
                    "Authorization": f"Bearer {token}"
                })

                response.success()
            else:
                response.failure(f"Auth failed: {response.text}")
                raise StopUser()

        if random.random() < 0.2:
            self.role = "fanatic"
        else:
            self.role = "moderate"

    @task
    def cast_batch_vote(self):
        """Отправка голоса"""
        if not self.user_id:
            raise StopUser()

        votes = {}

        if self.role == "moderate":
            votes[str(OPTION_A)] = 5
            votes[str(OPTION_B)] = 4
        else:
            votes[str(OPTION_B)] = 10

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

        raise StopUser()