/**
 * 错误处理工具
 * 用于处理第三方插件（如广告屏蔽插件）引起的错误
 */

// 全局错误处理器
window.addEventListener('error', function(event) {
    // 检查是否是广告屏蔽插件相关的错误
    if (event.filename && event.filename.includes('adpingbi.js')) {
        console.warn('检测到广告屏蔽插件错误，已忽略:', event.message);
        event.preventDefault();
        return false;
    }
    
    // 检查是否是miguanPlugin相关的错误
    if (event.message && event.message.includes('miguanPlugin')) {
        console.warn('检测到广告屏蔽插件错误，已忽略:', event.message);
        event.preventDefault();
        return false;
    }
    
    // 其他错误正常处理
    console.error('应用错误:', event.error);
});

// 处理未捕获的Promise错误
window.addEventListener('unhandledrejection', function(event) {
    // 检查是否是广告屏蔽插件相关的错误
    if (event.reason && event.reason.message && 
        (event.reason.message.includes('adpingbi') || 
         event.reason.message.includes('miguanPlugin'))) {
        console.warn('检测到广告屏蔽插件Promise错误，已忽略:', event.reason);
        event.preventDefault();
        return false;
    }
    
    // 其他Promise错误正常处理
    console.error('未处理的Promise错误:', event.reason);
});

// 安全的DOM操作函数
const safeDOM = {
    // 安全地设置元素样式
    setStyle: function(element, property, value) {
        try {
            if (element && element.style) {
                element.style[property] = value;
            }
        } catch (error) {
            console.warn('设置样式失败:', error.message);
        }
    },
    
    // 安全地显示元素
    show: function(element) {
        this.setStyle(element, 'display', 'block');
    },
    
    // 安全地隐藏元素
    hide: function(element) {
        this.setStyle(element, 'display', 'none');
    },
    
    // 安全地添加类
    addClass: function(element, className) {
        try {
            if (element && element.classList) {
                element.classList.add(className);
            }
        } catch (error) {
            console.warn('添加类失败:', error.message);
        }
    },
    
    // 安全地移除类
    removeClass: function(element, className) {
        try {
            if (element && element.classList) {
                element.classList.remove(className);
            }
        } catch (error) {
            console.warn('移除类失败:', error.message);
        }
    }
};

// 安全的jQuery扩展
if (typeof $ !== 'undefined') {
    // 扩展jQuery的安全方法
    $.fn.safeShow = function() {
        return this.each(function() {
            safeDOM.show(this);
        });
    };
    
    $.fn.safeHide = function() {
        return this.each(function() {
            safeDOM.hide(this);
        });
    };
    
    $.fn.safeAddClass = function(className) {
        return this.each(function() {
            safeDOM.addClass(this, className);
        });
    };
    
    $.fn.safeRemoveClass = function(className) {
        return this.each(function() {
            safeDOM.removeClass(this, className);
        });
    };
}

// 导出工具函数
window.errorHandler = {
    safeDOM: safeDOM,
    // 检查是否是广告屏蔽插件错误
    isAdBlockError: function(error) {
        return error && (
            (error.message && error.message.includes('adpingbi')) ||
            (error.message && error.message.includes('miguanPlugin')) ||
            (error.filename && error.filename.includes('adpingbi.js'))
        );
    }
}; 