#!/usr/bin/env python3
"""最小示例服务：只用 Python 标准库，验证 tools/verify 不依赖 FoodMap 或 Node 项目结构。

接口：
  GET  /health        -> 200 {"status": "ok"}
  POST /items         -> 201 {"id": N, "name": "..."}
  GET  /items/<id>    -> 200 或 404

用法：
  python3 server.py --port 19100 --log logs/access.log
"""

import argparse
import json
import os
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ITEMS = {}
LOCK = threading.Lock()
LOG_PATH = None


def write_log(line):
    """把访问记录追加到日志文件，供场景用 logfile 断言。"""
    if not LOG_PATH:
        return
    os.makedirs(os.path.dirname(LOG_PATH), exist_ok=True)
    with open(LOG_PATH, 'a', encoding='utf-8') as handle:
        handle.write(f'{line}\n')


class Handler(BaseHTTPRequestHandler):
    """最小路由：健康检查、创建条目、按 id 查询。"""

    def _respond(self, status, payload):
        body = json.dumps(payload).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)
        write_log(f'{self.command} {self.path} -> {status}')

    def do_GET(self):
        if self.path == '/health':
            self._respond(200, {'status': 'ok'})
            return
        if self.path.startswith('/items/'):
            item_id = self.path.rsplit('/', 1)[-1]
            item = ITEMS.get(item_id)
            if item is None:
                self._respond(404, {'error': 'not_found'})
                return
            self._respond(200, item)
            return
        self._respond(404, {'error': 'not_found'})

    def do_POST(self):
        if self.path != '/items':
            self._respond(404, {'error': 'not_found'})
            return
        length = int(self.headers.get('Content-Length', '0'))
        try:
            payload = json.loads(self.rfile.read(length) or b'{}')
        except json.JSONDecodeError:
            self._respond(400, {'error': 'invalid_json'})
            return
        name = str(payload.get('name', '')).strip()
        if not name:
            self._respond(400, {'error': 'name_required'})
            return
        with LOCK:
            item_id = str(len(ITEMS) + 1)
            ITEMS[item_id] = {'id': int(item_id), 'name': name}
            item = ITEMS[item_id]
        self._respond(201, item)

    def log_message(self, *_args):
        """关闭默认 stderr 访问日志，统一写 --log 指定的文件。"""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, default=19100)
    parser.add_argument('--log', default='logs/access.log')
    args = parser.parse_args()

    global LOG_PATH
    LOG_PATH = os.path.abspath(args.log)
    server = ThreadingHTTPServer(('127.0.0.1', args.port), Handler)
    print(f'listening on http://127.0.0.1:{args.port}, log={LOG_PATH}', flush=True)
    server.serve_forever()


if __name__ == '__main__':
    main()
