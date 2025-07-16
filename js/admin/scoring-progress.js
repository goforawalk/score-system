$(document).ready(function() {
    // 验证用户权限
    if (!auth.isLoggedIn() || !auth.isAdmin()) {
        console.log('权限验证失败 - 用户角色:', auth.getRole());
        window.location.href = '../index.html';
        return;
    }

    // 初始化页面
    initializeUserInfo();
    loadTaskList();

    // 绑定事件
    $('#taskSelect').on('change', function() {
        const taskId = $(this).val();
        if (taskId) {
            loadProgressData(taskId);
        } else {
            clearAllData();
        }
    });

    $('#refreshBtn').on('click', function() {
        const taskId = $('#taskSelect').val();
        if (taskId) {
            loadProgressData(taskId);
        } else {
            showError('请先选择评审任务');
        }
    });
});

function initializeUserInfo() {
    const userInfo = auth.getUserInfo();
    $('#currentUser').text(userInfo.username);
    $('#logoutBtn').on('click', function(e) {
        e.preventDefault();
        logoutWithCacheConfirm();
    });
}

// 加载任务列表（轻量级）
function loadTaskList() {
    showLoading();
    api.getTaskList()
        .then(response => {
            if (response.success) {
                const $select = $('#taskSelect');
                $select.find('option:not(:first)').remove();
                
                response.data.forEach(task => {
                    const statusText = task.status === 'active' ? '(进行中)' : '(已完成)';
                    $select.append(`
                        <option value="${task.id}">${task.name} ${statusText}</option>
                    `);
                });
                
                if (response.data.length === 0) {
                    $select.append('<option disabled>暂无可用评审任务</option>');
                }
            }
        })
        .catch(error => {
            showError('加载任务列表失败');
            console.error(error);
        })
        .finally(() => {
            hideLoading();
        });
}

// 加载进度数据
function loadProgressData(taskId) {
    showLoading();
    
    // 并行加载多个轻量级数据
    Promise.all([
        api.getScoringProgress(taskId),
        api.getProjectList(taskId),
        api.getProjectRanking(taskId)
    ])
    .then(([progressResponse, projectResponse, rankingResponse]) => {
        if (progressResponse.success && projectResponse.success && rankingResponse.success) {
            updateProgressOverview(progressResponse.data);
            updateCharts(progressResponse.data, projectResponse.data, rankingResponse.data);
            updateExpertDetails(taskId);
        } else {
            throw new Error('数据加载失败');
        }
    })
    .catch(error => {
        showError('加载进度数据失败: ' + error.message);
        console.error(error);
    })
    .finally(() => {
        hideLoading();
    });
}

// 更新进度概览
function updateProgressOverview(progressData) {
    $('#totalExperts').text(progressData.totalExperts);
    $('#totalProjects').text(progressData.totalProjects);
    $('#totalTasks').text(progressData.totalTasks);
    $('#completedScores').text(progressData.completedScores);
    $('#completionRate').text(progressData.completionRate.toFixed(1) + '%');
    
    // 更新进度条
    const progressPercent = progressData.completionRate;
    $('#progressFill').css('width', progressPercent + '%');
}

// 更新图表
function updateCharts(progressData, projectList, rankingList) {
    // 专家评分进度图表
    initExpertProgressChart(progressData);
    
    // 项目评分进度图表
    initProjectProgressChart(projectList, progressData);
    
    // 评分时间分布图表
    initTimeDistributionChart();
    
    // 评分状态统计图表
    initStatusChart(progressData);
}

// 专家评分进度图表
function initExpertProgressChart(progressData) {
    const chart = echarts.init(document.getElementById('expertProgressChart'));
    
    const option = {
        tooltip: {
            trigger: 'item',
            formatter: '{b}: {c} ({d}%)'
        },
        series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            data: [
                {
                    name: '已完成',
                    value: progressData.completedScores,
                    itemStyle: { color: '#52c41a' }
                },
                {
                    name: '未完成',
                    value: progressData.totalTasks - progressData.completedScores,
                    itemStyle: { color: '#ff4d4f' }
                }
            ],
            label: {
                show: true,
                formatter: '{b}: {c}'
            }
        }]
    };
    
    chart.setOption(option);
}

// 项目评分进度图表
function initProjectProgressChart(projectList, progressData) {
    const chart = echarts.init(document.getElementById('projectProgressChart'));
    
    // 计算每个项目的完成情况（简化处理）
    const projectData = projectList.map(project => ({
        name: project.name,
        value: Math.floor(Math.random() * 100) // 这里应该从后端获取实际数据
    }));
    
    const option = {
        tooltip: {
            trigger: 'axis',
            formatter: '{b}: {c}%'
        },
        xAxis: {
            type: 'category',
            data: projectData.map(item => item.name),
            axisLabel: {
                interval: 0,
                rotate: 30
            }
        },
        yAxis: {
            type: 'value',
            name: '完成率(%)',
            max: 100
        },
        series: [{
            type: 'bar',
            data: projectData.map(item => item.value),
            itemStyle: {
                color: function(params) {
                    const value = params.value;
                    if (value >= 80) return '#52c41a';
                    if (value >= 50) return '#faad14';
                    return '#ff4d4f';
                }
            }
        }]
    };
    
    chart.setOption(option);
}

// 评分时间分布图表
function initTimeDistributionChart() {
    const chart = echarts.init(document.getElementById('timeDistributionChart'));
    
    // 模拟时间分布数据
    const timeData = [
        { name: '上午', value: 35 },
        { name: '下午', value: 45 },
        { name: '晚上', value: 20 }
    ];
    
    const option = {
        tooltip: {
            trigger: 'item',
            formatter: '{b}: {c}%'
        },
        series: [{
            type: 'pie',
            radius: '50%',
            data: timeData,
            label: {
                show: true,
                formatter: '{b}: {c}%'
            }
        }]
    };
    
    chart.setOption(option);
}

// 评分状态统计图表
function initStatusChart(progressData) {
    const chart = echarts.init(document.getElementById('statusChart'));
    
    const statusData = [
        { name: '已完成', value: progressData.completedScores },
        { name: '进行中', value: Math.floor(progressData.totalTasks * 0.3) },
        { name: '未开始', value: progressData.totalTasks - progressData.completedScores - Math.floor(progressData.totalTasks * 0.3) }
    ];
    
    const option = {
        tooltip: {
            trigger: 'item',
            formatter: '{b}: {c} ({d}%)'
        },
        series: [{
            type: 'pie',
            radius: '50%',
            data: statusData,
            label: {
                show: true,
                formatter: '{b}: {c}'
            }
        }]
    };
    
    chart.setOption(option);
}

// 更新专家详情表格
function updateExpertDetails(taskId) {
    // 这里应该调用专门的API获取专家详情
    // 暂时使用模拟数据
    const expertData = [
        { expert: '专家A', scored: 8, total: 10, rate: 80, lastTime: '2024-01-15 14:30' },
        { expert: '专家B', scored: 6, total: 10, rate: 60, lastTime: '2024-01-15 16:20' },
        { expert: '专家C', scored: 10, total: 10, rate: 100, lastTime: '2024-01-15 18:45' }
    ];
    
    const $tbody = $('#expertDetailTable');
    $tbody.empty();
    
    expertData.forEach(expert => {
        $tbody.append(`
            <tr>
                <td>${expert.expert}</td>
                <td>${expert.scored}</td>
                <td>${expert.total}</td>
                <td>${expert.rate}%</td>
                <td>${expert.lastTime}</td>
            </tr>
        `);
    });
}

// 清空所有数据
function clearAllData() {
    // 清空进度概览
    $('#totalExperts').text('0');
    $('#totalProjects').text('0');
    $('#totalTasks').text('0');
    $('#completedScores').text('0');
    $('#completionRate').text('0%');
    $('#progressFill').css('width', '0%');
    
    // 清空图表
    ['expertProgressChart', 'projectProgressChart', 'timeDistributionChart', 'statusChart'].forEach(chartId => {
        const chart = echarts.getInstanceByDom(document.getElementById(chartId));
        if (chart) {
            chart.clear();
        }
    });
    
    // 清空表格
    $('#expertDetailTable').empty();
}

// 显示加载状态
function showLoading() {
    $('.loading-overlay').fadeIn(200);
}

// 隐藏加载状态
function hideLoading() {
    $('.loading-overlay').fadeOut(200);
}

// 显示错误信息
function showError(message) {
    alert(message);
}

// 窗口大小改变时调整图表
$(window).on('resize', function() {
    ['expertProgressChart', 'projectProgressChart', 'timeDistributionChart', 'statusChart'].forEach(chartId => {
        const chart = echarts.getInstanceByDom(document.getElementById(chartId));
        if (chart) {
            chart.resize();
        }
    });
}); 