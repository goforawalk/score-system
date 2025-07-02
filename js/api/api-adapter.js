/**
 * API适配器
 * 根据环境配置，选择使用模拟API或真实API
 */

const api = (function() {
    // 私有变量，存储当前使用的API实现
    var currentApi = null;
    
    /**
     * 初始化API适配器
     */
    function initialize() {
        // 根据环境配置选择API实现
        if (env.useMockApi) {
            currentApi = mockApi;
            console.log('使用模拟API');
        } else {
            currentApi = apiService;
            console.log('使用真实API');
        }
    }
    
    /**
     * 切换API实现
     * @param {boolean} useMock 是否使用模拟API
     */
    function switchApi(useMock) {
        env.useMockApi = useMock;
        initialize();
    }
    
    // 初始化
    initialize();
    
    // 返回代理对象，将所有方法调用转发到当前API实现
    return new Proxy({
        // 公开的方法
        switchToMock: function() {
            switchApi(true);
        },
        switchToReal: function() {
            switchApi(false);
        },
        getCurrentMode: function() {
            return env.useMockApi ? 'mock' : 'real';
        }
    }, {
        // 代理处理器
        get: function(target, prop) {
            // 如果是公开的方法，直接返回
            if (prop in target) {
                return target[prop];
            }
            
            // 如果当前API实现有该方法，转发调用
            if (currentApi && typeof currentApi[prop] === 'function') {
                return function() {
                    return currentApi[prop].apply(currentApi, arguments);
                };
            }
            
            // 如果当前API实现有该属性，返回属性值
            if (currentApi && prop in currentApi) {
                return currentApi[prop];
            }
            
            // 否则返回undefined
            return undefined;
        }
    });
})(); 