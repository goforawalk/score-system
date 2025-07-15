$(function() {
    const $btn = $('#generate-btn');
    const $log = $('#generator-log');

    function log(msg) {
        $log.append('<div>' + msg + '</div>');
        $log.scrollTop($log[0].scrollHeight);
    }
    function clearLog() {
        $log.html('');
    }

    $btn.on('click', function() {
        $btn.prop('disabled', true);
        clearLog();
        log('开始生成测试数据…');
        // 发送请求到后端生成测试数据接口
        $.ajax({
            url: 'http://localhost:8080/api/test-data/generate',
            method: 'POST',
            contentType: 'application/json',
            success: function(res) {
                log('测试数据生成成功！');
                log('详情：' + (res && res.message ? res.message : JSON.stringify(res)));
            },
            error: function(xhr) {
                log('生成失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : xhr.statusText));
            },
            complete: function() {
                $btn.prop('disabled', false);
            }
        });
    });
}); 