$(document).ready(function() {
    // 验证用户权限
    if (!auth.isLoggedIn() || auth.getRole() !== 'admin') {
        window.location.href = '../index.html';
        return;
    }

    // 初始化用户信息
    initializeUserInfo();
    
    // 加载项目列表
    loadProjects();

    // 绑定项目选择事件
    $('#projectSelect').on('change', function() {
        const projectId = $(this).val();
        if (projectId) {
            loadProjectScores(projectId);
        } else {
            clearScoreDisplay();
        }
    });
});

// 添加窗口大小改变时的图表自适应
$(window).on('resize', function() {
    if (scoreChart) {
        scoreChart.resize();
    }
});

function initializeUserInfo() {
    const userInfo = auth.getUserInfo();
    $('#currentUser').text(userInfo.username);
    $('#logoutBtn').on('click', function(e) {
        e.preventDefault();
        auth.removeUserInfo();
        window.location.href = '../index.html';
    });
}

function loadProjects() {
    mockApi.getProjects()
        .then(response => {
            if (response.success) {
                const $select = $('#projectSelect');
                $select.find('option:not(:first)').remove();
                
                response.data.forEach(project => {
                    $select.append(`
                        <option value="${project.id}">${project.name}</option>
                    `);
                });
            }
        })
        .catch(error => {
            alert('加载项目列表失败');
            console.error(error);
        });
}

// 修改 loadProjectScores 函数
async function loadProjectScores(projectId) {
    try {
        showLoading();
        
        const response = await mockApi.getProjectScores(projectId);
        
        if (response.success) {
            const data = response.data;
            
            // 渲染评分统计
            renderScoreStats(data);
            
            // 渲染评分详情表格
            if (data.scoreItems && data.details) {
                renderScoreDetails(data.details, data.scoreItems);
            }
            
            // 渲染评分分布图表
            initScoreDistributionChart(data);
            
            // 更新进度信息
            if (data.progress) {
                renderScoreProgress(data.progress);
            }
        } else {
            showError('加载评分数据失败：' + (response.message || '未知错误'));
        }
    } catch (error) {
        console.error('加载项目评分数据失败:', error);
        showError('加载评分数据失败：' + (error.message || '未知错误'));
    } finally {
        hideLoading();
    }
}

// 添加完成率计算函数
function calculateCompletionRate(progressData) {
    try {
        if (!progressData || !progressData.total || progressData.total === 0) {
            return 0;
        }

        const completedCount = progressData.completed || 0;
        const totalCount = progressData.total;
        
        // 计算完成率并转换为百分比
        const rate = (completedCount / totalCount) * 100;
        
        // 确保返回值在0-100之间
        return Math.min(100, Math.max(0, rate));
    } catch (error) {
        console.error('计算完成率失败:', error);
        return 0;
    }
}

function renderScoreProgress(progress) {
    const $progressSection = $('.progress-section');
    if (!$progressSection.length) {
        // 如果进度条区域不存在，创建它
        $('.stats-card').after(`
            <div class="progress-section">
                <h3>评分进度</h3>
                <div class="progress-info">
                    <div class="progress-bar-container">
                        <div class="progress-bar" id="totalProgress"></div>
                    </div>
                    <div class="progress-text" id="progressText"></div>
                </div>
            </div>
        `);
    }

    const percentage = (progress.scored / progress.total * 100) || 0;
    $('#totalProgress').css('width', `${percentage}%`);
    $('#progressText').text(
        `${progress.scored}/${progress.total} 项评分已完成 (${percentage.toFixed(1)}%)`
    );

    // 根据完成度设置进度条颜色
    const $progress = $('#totalProgress');
    if (percentage < 30) {
        $progress.removeClass('progress-medium progress-high').addClass('progress-low');
    } else if (percentage < 70) {
        $progress.removeClass('progress-low progress-high').addClass('progress-medium');
    } else {
        $progress.removeClass('progress-low progress-medium').addClass('progress-high');
    }
}

// 修改 renderScoreStats 函数
function renderScoreStats(data) {
    const $stats = $('#scoreStats');
    $stats.empty();

    if (!data || !data.statistics) return;

    // 添加总分统计
    $stats.append(`
        <div class="stat-item">
            <span>项目总分</span>
            <span class="total-score">${data.statistics.averageScore.toFixed(2)}</span>
        </div>
    `);

    // 如果有评分详情，添加最高分和最低分
    if (data.details && data.details.length > 0) {
        const scores = [];
        data.details.forEach(detail => {
            if (detail.scores) {
                detail.scores.forEach(score => {
                    if (!isNaN(score.score)) {
                        scores.push(score.score);
                    }
                });
            }
        });

        if (scores.length > 0) {
            const maxScore = Math.max(...scores);
            const minScore = Math.min(...scores);

            $stats.append(`
                <div class="stat-item">
                    <span>最高单项分</span>
                    <span>${maxScore.toFixed(1)}</span>
                </div>
                <div class="stat-item">
                    <span>最低单项分</span>
                    <span>${minScore.toFixed(1)}</span>
                </div>
            `);
        }
    }
}

// 修改 renderScoreDetails 函数
function renderScoreDetails(details, scoreItems) {
    const $scoreList = $('#scoreList');
    $scoreList.empty();

    if (!Array.isArray(details)) return;

    details.forEach(detail => {
        if (detail.scores && Array.isArray(detail.scores)) {
            detail.scores.forEach(score => {
                // 找到对应的评分项信息
                const scoreItem = scoreItems.find(item => item.name);
                $scoreList.append(`
                    <tr>
                        <td>${detail.username || '未知用户'}</td>
                        <td>${scoreItem ? scoreItem.name : '未知评分项'}</td>
                        <td>${score.score || 0}</td>
                        <td>${new Date(detail.submitTime).toLocaleString()}</td>
                    </tr>
                `);
            });
        }
    });
}

function clearScoreDisplay() {
    $('#scoreStats').empty();
    $('#scoreList').empty();
    
    // 正确清除图表
    if (scoreChart) {
        scoreChart.dispose();
        scoreChart = null;
    }
    
    $('#totalProgress').css('width', '0%');
    $('#progressText').text('');
}

function checkScoringDelay() {
    const projectId = $('#projectSelect').val();
    if (!projectId) return;

    mockApi.getProjectScores(projectId)
        .then(response => {
            if (response.success) {
                const threshold = 30 * 60 * 1000; // 30分钟
                const now = new Date().getTime();
                
                response.data.details.forEach(detail => {
                    const lastScoreTime = new Date(detail.submitTime).getTime();
                    if (now - lastScoreTime > threshold) {
                        notifyExpert(detail.username);
                    }
                });
            }
        });
}

function notifyExpert(username) {
    // 发送提醒消息
    console.log(`提醒专家 ${username} 继续评分`);
}

let scoreChart = null; // 添加全局变量存储图表实例

// 修改图表初始化函数
function initScoreDistributionChart(data) {
    if (!data || !data.details || !Array.isArray(data.details)) return;

    const chartDom = document.getElementById('scoreChart');
    if (!chartDom) return;

    if (scoreChart) {
        scoreChart.dispose();
    }

    scoreChart = echarts.init(chartDom);

    const scoreRanges = [
        { range: '90-100', count: 0 },
        { range: '80-89', count: 0 },
        { range: '70-79', count: 0 },
        { range: '60-69', count: 0 },
        { range: '0-59', count: 0 }
    ];

    // 收集所有有效分数
    const scores = [];
    data.details.forEach(detail => {
        if (detail.scores && Array.isArray(detail.scores)) {
            detail.scores.forEach(score => {
                if (!isNaN(score.score)) {
                    scores.push(score.score);
                }
            });
        }
    });

    // 统计分数分布
    scores.forEach(score => {
        if (score >= 90) scoreRanges[0].count++;
        else if (score >= 80) scoreRanges[1].count++;
        else if (score >= 70) scoreRanges[2].count++;
        else if (score >= 60) scoreRanges[3].count++;
        else scoreRanges[4].count++;
    });

    const option = {
        title: {
            text: '评分分布',
            left: 'center'
        },
        tooltip: {
            trigger: 'item',
            formatter: '{b}: {c} ({d}%)'
        },
        series: [{
            name: '评分分布',
            type: 'pie',
            radius: '50%',
            data: scoreRanges.map(item => ({
                name: item.range,
                value: item.count
            })),
            emphasis: {
                itemStyle: {
                    shadowBlur: 10,
                    shadowOffsetX: 0,
                    shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
            }
        }]
    };

    scoreChart.setOption(option);
}

function updateScoreDistributionChart(details) {
    const chart = echarts.getInstanceByDom(document.getElementById('scoreDistribution'));
    if (!chart) return;

    const scoreRanges = [0, 60, 70, 80, 90, 100];
    const distribution = Array(scoreRanges.length - 1).fill(0);

    details.forEach(detail => {
        const score = detail.score;
        for (let i = 0; i < scoreRanges.length - 1; i++) {
            if (score >= scoreRanges[i] && score < scoreRanges[i + 1]) {
                distribution[i]++;
                break;
            }
        }
    });

    chart.setOption({
        series: [{
            data: distribution
        }]
    });
}

function detectAbnormalScores() {
    const projectId = $('#projectSelect').val();
    if (!projectId) return;

    mockApi.getProjectScores(projectId)
        .then(response => {
            if (response.success) {
                const scores = response.data.details;
                const avgScore = response.data.averageScore;
                const threshold = 20; // 设置分差阈值

                scores.forEach(score => {
                    const diff = Math.abs(score.score - avgScore);
                    if (diff > threshold) {
                        markAbnormalScore(score);
                    }
                });
            }
        });
}

function markAbnormalScore(score) {
    const $row = $(`.score-detail[data-username="${score.username}"]`);
    $row.addClass('abnormal')
        .append('<span class="warning-icon" title="评分异常">⚠️</span>');
}

// 修改 updateProgressBar 函数
function updateProgressBar(progressData) {
    try {
        if (!progressData || typeof progressData.total !== 'number' || typeof progressData.scored !== 'number') {
            console.warn('无效的进度数据:', progressData);
            return;
        }

        // 计算完成率百分比
        const percentage = (progressData.total > 0) ? 
            Math.floor((progressData.scored / progressData.total) * 100) : 0;

        // 更新进度条
        $('#totalProgress')
            .css('width', `${percentage}%`)
            .removeClass('progress-low progress-medium progress-high')
            .addClass(getProgressClass(percentage));

        // 更新进度文本
        $('#progressText').text(
            `已完成 ${progressData.scored}/${progressData.total} 项评分 (${percentage}%)`
        );
    } catch (error) {
        console.error('更新进度条失败:', error);
    }
}

// 添加进度颜色辅助函数
function getProgressClass(percentage) {
    if (percentage < 30) return 'progress-low';
    if (percentage < 70) return 'progress-medium';
    return 'progress-high';
}

// 添加错误提示函数
function showErrorMessage(message) {
    // 如果页面中有用于显示错误的元素，则更新它
    const $errorTip = $('.error-tip');
    if ($errorTip.length) {
        $errorTip.text(message).fadeIn();
        setTimeout(() => $errorTip.fadeOut(), 3000);
    } else {
        // 否则使用 alert
        alert(message);
    }
}

// 辅助函数
function showLoading() {
    // TODO: 添加加载状态显示
}

function hideLoading() {
    // TODO: 移除加载状态显示
}

function showError(message) {
    const $error = $('.error-tip');
    $error.text(message).fadeIn();
    setTimeout(() => $error.fadeOut(), 3000);
}
