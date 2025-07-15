// 自动化测试：测试数据生成接口
(async function() {
    function log(msg) { console.log('[TestDataGeneratorTest]', msg); }
    log('开始测试 /api/test-data/generate 接口…');
    try {
        const res = await fetch('/api/test-data/generate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const data = await res.json();
        log('接口返回：' + JSON.stringify(data));
        // 这里可根据实际返回结构做更细致的断言
        if (!data || !data.success) throw new Error('success 字段为 false 或缺失');
        log('测试通过！');
    } catch (e) {
        log('测试失败：' + e.message);
    }
})(); 