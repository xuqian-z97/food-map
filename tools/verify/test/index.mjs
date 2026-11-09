// Node 24 把显式的目录参数当作模块路径解析；用 package.json main 指向这里，
// 让验收命令 `node --test tools/verify/test/` 仍能运行完整测试集。
import './runner.test.mjs';
import './scenario.test.mjs';
import './features.test.mjs';
import './deps.test.mjs';
