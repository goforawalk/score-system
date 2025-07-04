/**
 * HTTP工具类，用于处理API请求
 * 包括GET、POST、PUT、DELETE方法，以及处理认证令牌和错误
 */

const http = {
    /**
     * 获取认证令牌
     * @returns {string|null} 认证令牌
     */
    getAuthToken: function() {
        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            return userInfo.token || null;
        } catch (e) {
            console.error('获取认证令牌失败:', e);
            return null;
        }
    },

    /**
     * 构建完整的API URL
     * @param {string} endpoint API端点
     * @returns {string} 完整的API URL
     */
    buildUrl: function(endpoint) {
        // 如果endpoint已经是完整URL，则直接返回
        if (endpoint.startsWith('http://') || endpoint.startsWith('https://')) {
            return endpoint;
        }
        
        // 确保endpoint不以斜杠开头，避免重复斜杠
        if (endpoint.startsWith('/') && apiConfig.baseUrl.endsWith('/')) {
            endpoint = endpoint.substring(1);
        }
        
        return apiConfig.baseUrl + endpoint;
    },

    /**
     * 构建请求头
     * @param {Object} customHeaders 自定义请求头
     * @returns {Object} 请求头对象
     */
    buildHeaders: function(customHeaders) {
        customHeaders = customHeaders || {};
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        };

        // 合并自定义请求头
        Object.keys(customHeaders).forEach(function(key) {
            headers[key] = customHeaders[key];
        });

        const token = this.getAuthToken();
        if (token) {
            headers['Authorization'] = 'Bearer ' + token;
        }

        return headers;
    },

    /**
     * 处理API响应
     * @param {Response} response Fetch API响应对象
     * @returns {Promise} 处理后的响应数据
     */
    handleResponse: function(response) {
        // 处理空响应
        if (response.status === 204) {
            return Promise.resolve({ success: true, message: "操作成功", data: null });
        }
        
        // 处理403错误
        if (response.status === 403) {
            console.error("CORS或权限错误: 服务器返回403 Forbidden");
            console.log("请求URL:", response.url);
            console.log("请求方法:", response.type);
            console.log("响应头:", [...response.headers.entries()]);
            
            // 检查是否为CORS预检请求问题
            if (response.type === 'opaque' || !response.headers.get('Access-Control-Allow-Origin')) {
                return Promise.reject({
                    success: false,
                    message: "CORS配置错误，服务器未允许跨域请求",
                    code: "CORS_ERROR"
                });
            }
            
            return Promise.reject({
                success: false,
                message: "服务器拒绝访问，可能是权限不足或CORS配置问题",
                code: "FORBIDDEN_ERROR"
            });
        }
        
        // 处理401未授权错误
        if (response.status === 401) {
            console.log("未授权访问，可能需要登录");
            // 只在非登录页面清除用户信息并跳转
            if (!window.location.href.includes('login.html') && !window.location.href.includes('index.html')) {
                localStorage.removeItem('userInfo');
                window.location.href = '/index.html';
            }
            
            return Promise.reject({
                success: false,
                message: "未授权访问，请登录",
                code: "UNAUTHORIZED_ERROR"
            });
        }
        
        // 尝试解析JSON响应
        return response.text().then(function(text) {
            console.log('响应文本内容:', text);
            console.log('响应文本长度:', text ? text.length : 0);
            
            // 检查响应是否为空
            if (!text || text.trim() === '') {
                console.log('响应为空，状态码:', response.status);
                if (!response.ok) {
                    throw {
                        status: response.status,
                        message: "服务器返回了空响应",
                        code: "EMPTY_RESPONSE"
                    };
                }
                return { success: true, message: "操作成功", data: null };
            }
            
            try {
                // 尝试解析JSON
                const data = JSON.parse(text);
                console.log('解析后的JSON数据:', data);
                
                if (!response.ok) {
                    throw {
                        status: response.status,
                        message: data.message || '请求失败',
                        code: data.code,
                        data: data.data
                    };
                }
                
                return data;
            } catch (e) {
                console.error("JSON解析错误:", e, "原始响应:", text);
                throw {
                    status: response.status,
                    message: "无法解析服务器响应",
                    code: "JSON_PARSE_ERROR",
                    originalText: text
                };
            }
        });
    },

    /**
     * 处理API错误
     * @param {Error} error 错误对象
     * @throws {Object} 格式化的错误对象
     */
    handleError: function(error) {
        console.error('API请求错误:', error);
        
        // 网络错误
        if (error.name === 'TypeError' && error.message === 'Failed to fetch') {
            throw {
                success: false,
                message: '网络连接错误，请检查网络连接或后端服务是否启动',
                code: 'NETWORK_ERROR'
            };
        }
        
        // API返回的错误
        if (error.status) {
            throw {
                success: false,
                message: error.message,
                code: error.code,
                data: error.data
            };
        }
        
        // 其他错误
        throw {
            success: false,
            message: error.message || '未知错误',
            code: 'UNKNOWN_ERROR'
        };
    },

    /**
     * 发送GET请求
     * @param {string} endpoint API端点
     * @param {Object} params URL参数
     * @param {Object} customHeaders 自定义请求头
     * @returns {Promise} 响应数据
     */
    get: function(endpoint, params, customHeaders) {
        var self = this;
        params = params || {};
        customHeaders = customHeaders || {};
        
        try {
            // 构建查询字符串
            var queryParts = [];
            Object.keys(params).forEach(function(key) {
                queryParts.push(encodeURIComponent(key) + '=' + encodeURIComponent(params[key]));
            });
            
            var queryString = queryParts.length > 0 ? '?' + queryParts.join('&') : '';
            var url = this.buildUrl(endpoint + queryString);
            
            console.log('发送GET请求到:', url);
            
            return fetch(url, {
                method: 'GET',
                headers: this.buildHeaders(customHeaders),
                credentials: 'include', // 添加跨域Cookie支持
                mode: 'cors' // 明确指定跨域模式
            })
            .then(function(response) {
                return self.handleResponse(response);
            })
            .catch(function(error) {
                return self.handleError(error);
            });
        } catch (error) {
            return Promise.reject(this.handleError(error));
        }
    },

    /**
     * 发送POST请求
     * @param {string} endpoint API端点
     * @param {Object} data 请求数据
     * @param {Object} customHeaders 自定义请求头
     * @returns {Promise} 响应数据
     */
    post: function(endpoint, data, customHeaders) {
        var self = this;
        data = data || {};
        customHeaders = customHeaders || {};
        
        try {
            console.log('发送POST请求到:', this.buildUrl(endpoint), '数据:', data);
            
            return fetch(this.buildUrl(endpoint), {
                method: 'POST',
                headers: this.buildHeaders(customHeaders),
                body: JSON.stringify(data),
                credentials: 'include', // 添加跨域Cookie支持
                mode: 'cors' // 明确指定跨域模式
            })
            .then(function(response) {
                console.log('收到POST响应:', response);
                return self.handleResponse(response);
            })
            .catch(function(error) {
                console.error('POST请求错误:', error);
                return self.handleError(error);
            });
        } catch (error) {
            console.error('POST请求异常:', error);
            return Promise.reject(this.handleError(error));
        }
    },

    /**
     * 发送PUT请求
     * @param {string} endpoint API端点
     * @param {Object} data 请求数据
     * @param {Object} customHeaders 自定义请求头
     * @returns {Promise} 响应数据
     */
    put: function(endpoint, data, customHeaders) {
        var self = this;
        data = data || {};
        customHeaders = customHeaders || {};
        
        try {
            return fetch(this.buildUrl(endpoint), {
                method: 'PUT',
                headers: this.buildHeaders(customHeaders),
                body: JSON.stringify(data),
                credentials: 'include', // 添加跨域Cookie支持
                mode: 'cors' // 明确指定跨域模式
            })
            .then(function(response) {
                return self.handleResponse(response);
            })
            .catch(function(error) {
                return self.handleError(error);
            });
        } catch (error) {
            return Promise.reject(this.handleError(error));
        }
    },

    /**
     * 发送DELETE请求
     * @param {string} endpoint API端点
     * @param {Object} customHeaders 自定义请求头
     * @returns {Promise} 响应数据
     */
    delete: function(endpoint, customHeaders) {
        var self = this;
        customHeaders = customHeaders || {};
        
        try {
            return fetch(this.buildUrl(endpoint), {
                method: 'DELETE',
                headers: this.buildHeaders(customHeaders),
                credentials: 'include', // 添加跨域Cookie支持
                mode: 'cors' // 明确指定跨域模式
            })
            .then(function(response) {
                return self.handleResponse(response);
            })
            .catch(function(error) {
                return self.handleError(error);
            });
        } catch (error) {
            return Promise.reject(this.handleError(error));
        }
    }
};
