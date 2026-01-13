import random
from locust import HttpUser, task, between

class QVUser(HttpUser):
    wait_time = between(1, 3)

    @task
    def cast_vote(self):
        user_id = 101
        project_id = random.randint(1, 10)
        vote_count = random.randint(1, 3)

        payload = {
            "userId": user_id,
            "projectId": project_id,
            "voteCount": vote_count
        }

        with self.client.post("/api/votes", json=payload, catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status code: {response.status_code}")