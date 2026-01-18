#!/usr/bin/env python3
import subprocess
import re
import time
import sys

def run_command(cmd, capture_output=True, text=True):
    """Запускает команду и возвращает результат"""
    try:
        result = subprocess.run(cmd, shell=True, capture_output=capture_output, text=text, check=True)
        return result.stdout.strip() if capture_output else None
    except subprocess.CalledProcessError as e:
        print(f"Ошибка выполнения команды '{cmd}': {e}")
        print(f"stderr: {e.stderr}")
        sys.exit(1)

def wait_for_docker_compose():
    """Ждет завершения docker-compose up"""
    print("Запуск docker-compose up -d --build...")
    run_command("docker-compose up -d --build", capture_output=False)

    print("Ожидание готовности контейнеров...")
    max_wait = 300
    waited = 0

    while waited < max_wait:
        status = run_command("docker-compose ps")
        if "Up" in status and "qv-geth-1" in status and "qv-geth-2" in status:
            print("Все контейнеры запущены!")
            break
        print(f"Ожидание... ({waited}s)")
        time.sleep(5)
        waited += 5
    else:
        print("Таймаут ожидания контейнеров")
        sys.exit(1)

def get_enode():
    """Получает enode из первого geth нода"""
    print("Получение enode от qv-geth-1...")
    output = run_command("docker exec qv-geth-1 geth attach --exec admin.nodeInfo.enode")

    match = re.search(r'enode://([^@]+)', output)
    if not match:
        print(f"Не удалось найти enode в выводе: {output}")
        sys.exit(1)

    node_id = match.group(1)
    full_enode = f"enode://{node_id}@geth-node1:30303"
    print(f"Полный enode: {full_enode}")
    return full_enode

def add_peer(enode):
    """Добавляет peer ко второму ноду"""
    peer_cmd = f"docker exec qv-geth-2 geth attach --exec \"admin.addPeer('{enode}')\""
    print(f"Добавление peer: {peer_cmd}")
    result = run_command(peer_cmd)
    print(f"Результат: {result}")

def check_peer_count():
    """Проверяет количество пиров"""
    print("Проверка количества пиров...")
    peer_count = run_command("docker exec qv-geth-1 geth attach --exec net.peerCount")
    print(f"Количество пиров на qv-geth-1: {peer_count}")

def main():
    print("🚀 Автоматическое развертывание Ethereum нодов")
    print("=" * 50)

    wait_for_docker_compose()
    enode = get_enode()
    add_peer(enode)
    check_peer_count()

    print("\n✅ Развертывание завершено успешно!")

if __name__ == "__main__":
    main()