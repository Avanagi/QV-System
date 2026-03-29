#!/usr/bin/env python3
import subprocess
import re
import sys
import json

def run_command(cmd, capture_output=True, encoding='utf-8'):
    """Execute command with proper encoding and error handling."""
    try:
        if capture_output:
            result = subprocess.run(
                cmd, shell=True,
                capture_output=True,
                text=True,
                encoding=encoding,
                check=True
            )
            return result.stdout.strip()
        else:
            subprocess.run(cmd, shell=True, check=True, capture_output=False)
            return None
    except subprocess.CalledProcessError as e:
        print(f"Ошибка выполнения команды '{cmd}': {e}")
        if capture_output and hasattr(e, 'stdout'):
            print(f"stdout: {e.stdout}")
        if capture_output and hasattr(e, 'stderr'):
            print(f"stderr: {e.stderr}")
        sys.exit(1)
    except UnicodeDecodeError as e:
        print(f"Ошибка декодирования вывода команды '{cmd}': {e}")
        sys.exit(1)

def deploy_nodes():
    """Быстрый запуск без ожидания."""
    print("🚀 Запуск docker-compose up -d --build...")
    run_command("docker-compose up -d --build", capture_output=False)

def get_enode():
    """Extract enode from qv-geth-1 container."""
    print("🔗 Получение enode от qv-geth-1...")
    cmd = "docker exec qv-geth-1 geth attach --exec admin.nodeInfo.enode"
    output = run_command(cmd)

    match = re.search(r'enode://([0-9a-fA-F]{128,})@', output)
    if not match:
        print(f"❌ Не удалось найти enode в выводе: {output[:200]}...")
        sys.exit(1)

    node_id = match.group(1)
    full_enode = f"enode://{node_id}@geth-node1:30303"
    print(f"✅ Полный enode: {full_enode}")
    return full_enode

def connect_peer(enode):
    """Connect peer to qv-geth-2."""
    peer_cmd = f"docker exec qv-geth-2 geth attach --exec \"admin.addPeer('{enode}')\""
    print("🔗 Подключение пиров...")
    result = run_command(peer_cmd)
    print(f"✅ Результат: {result}")

def show_status():
    """Quick status check."""
    print("\n📊 Быстрая проверка статуса:")
    try:
        peer_count = run_command("docker exec qv-geth-1 geth attach --exec net.peerCount")
        print(f"   Пиры на geth-1: {peer_count}")
    except:
        print("   Пиры: проверка позже (geth еще инициализируется)")

    print("\nДля детальной проверки:")
    print("  docker-compose ps")
    print("  docker-compose logs qv-geth-1")
    print("  docker-compose logs qv-geth-2")

def main():
    print("⚡ БЫСТРОЕ развертывание Ethereum нодов")
    print("=" * 50)

    deploy_nodes()
    print("⏳ Контейнеры запущены в фоне...")

    try:
        enode = get_enode()
        connect_peer(enode)
        show_status()
        print("\n🎉 Готово! Ноды работают (пиры подключатся через несколько секунд)")
    except KeyboardInterrupt:
        print("\n⏹️ Прервано пользователем")
    except Exception as e:
        print(f"\n❌ Ошибка: {e}")

if __name__ == "__main__":
    main()
