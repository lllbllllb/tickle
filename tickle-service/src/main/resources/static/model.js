export class UrlProvider {
    constructor(location) {
        this._httpProtocol = "http";
        this._wsProtocol = "ws";
        this._host = location.host;
        this._preyPath = "prey";
        this._ticklePath = "tickle";
        this._loadParametersPath = "loadParameters";
        this._loadWsPath = "websocket/load";
        this._countdownWsPath = "websocket/countdown";
    };

    get preyUrl() {
        return `${this._httpProtocol}://${this._host}/${this._preyPath}`;
    }

    getPreyUrl(name) {
        return `${this._httpProtocol}://${this._host}/${this._preyPath}/${name}`;
    }

    get tickleUrl() {
        return `${this._httpProtocol}://${this._host}/${this._preyPath}/${this._ticklePath}`;
    }

    get loadParametersUrl() {
        return `${this._httpProtocol}://${this._host}/${this._loadParametersPath}`;
    }

    getLoadWsUrl(name) {
        return `${this._wsProtocol}://${this._host}/${this._loadWsPath}?${name}`;
    }

    getCountdownWsUrl(name) {
        return `${this._wsProtocol}://${this._host}/${this._countdownWsPath}?${name}`;
    }
}

export class ColorGroup {
    constructor(borderColor, backgroundColor) {
        this._borderColor = borderColor;
        this._backgroundColor = backgroundColor;
    }

    get borderColor() {
        return this._borderColor;
    }

    get backgroundColor() {
        return this._backgroundColor;
    }
}

export class ColorPack {
    constructor(success, timeout, error) {
        this._success = success;
        this._timeout = timeout;
        this._error = error;
    }

    get success() {
        return this._success;
    }

    get timeout() {
        return this._timeout;
    }

    get error() {
        return this._error;
    }
}

export class Prey {
    constructor(name, path, method, requestParameters, headers, requestBody, timeoutMs, expectedResponseStatusCode, enabled) {
        this.name = name;
        this.path = path;
        this.method = method;
        this.requestParameters = requestParameters;
        this.headers = headers;
        this.requestBody = requestBody;
        this.timeoutMs = timeoutMs;
        this.expectedResponseStatusCode = expectedResponseStatusCode;
        this.enabled = enabled;
    }

}

export class LineChartContainer {
    constructor(chartId, name) {
        this._chartId = chartId;
        this._name = name;
        this._attemptNumbers = [];
        this._responseTimes = [];
        this._pointBackgroundColors = [];
        this._pointBorderColors = [];
        this._chart = new Chart(chartId, {
            type: "line",
            data: {
                labels: this._attemptNumbers,
                datasets: [
                    {
                        label: 'Response time, ms',
                        fill: false,
                        pointBackgroundColor: this._pointBackgroundColors,
                        pointBorderColor: this._pointBorderColors,
                        pointStyle: 'circle',
                        pointRadius: 8,
                        pointHoverRadius: 15,
                        data: this._responseTimes,
                        yAxisID: "axisResponseTime"
                    }
                ]
            },
            options: {
                responsive: true,
                legend: {
                    display: true
                },
                animation: false,
                showLine: false, // disable for all datasets
                scales: {
                    axisResponseTime: {
                        type: 'linear',
                        display: true,
                        position: 'left',
                        min: 0,
                        text: 'response time'
                    },
                    x: {
                        display: false
                    }
                },
                plugins: {
                    title: {
                        display: false,
                        text: name,
                    },
                    decimation: {
                        enabled: true,
                        algorithm: 'lttb',
                        samples: 4,
                        threshold: 1000
                    }
                }
            }
        });
    }

    addAttemptNumber(attemptNumber) {
        this._attemptNumbers.push(attemptNumber);
    }

    addResponseTime(responseTime) {
        this._responseTimes.push(responseTime);
    }

    addPointColorPack(colorGroup) {
        this._pointBackgroundColors.push(colorGroup.backgroundColor);
        this._pointBorderColors.push(colorGroup.borderColor)
    }

    get chart() {
        return this._chart;
    }

    get chartId() {
        return this._chartId;
    }

    get name() {
        return this._name;
    }

    reset() {
        this._attemptNumbers.splice(0, this._attemptNumbers.length);
        this._responseTimes.splice(0, this._responseTimes.length);
        this._pointBackgroundColors.splice(0, this._pointBackgroundColors.length);
        this._pointBorderColors.splice(0, this._pointBorderColors.length);
        this._chart.reset();
    }
}

export class BarChartContainer {
    constructor(chartId, name, colorPack) {
        this._chartId = chartId;
        this._name = name;
        this._successCount = [0];
        this._timeoutCount = [0];
        this._errorCount = [0];
        this._colorPack = colorPack;
        this._chart = new Chart(chartId, {
            type: "bar",
            data: {
                labels: ['Count'],
                datasets: [
                    {
                        label: 'Success',
                        data: this._successCount,
                        borderColor: this._colorPack.success.borderColor,
                        backgroundColor: this._colorPack.success.backgroundColor,
                        borderWidth: 2,
                        borderRadius: 5,
                        borderSkipped: false,
                    },
                    {
                        label: 'Timeout',
                        data: this._timeoutCount,
                        borderColor: this._colorPack.timeout.borderColor,
                        backgroundColor: this._colorPack.timeout.backgroundColor,
                        borderWidth: 2,
                        borderRadius: 5,
                        borderSkipped: false,
                    },
                    {
                        label: 'Error',
                        data: this._errorCount,
                        borderColor: this._colorPack.error.borderColor,
                        backgroundColor: this._colorPack.error.backgroundColor,
                        borderWidth: 2,
                        borderRadius: 5,
                        borderSkipped: false,
                    }
                ]
            },
            options: {
                responsive: true,
                animation: false,
                scales: {
                    x: {
                        display: false,
                    },
                    y: {
                        display: true,
                        type: 'logarithmic',
                    }
                },
                plugins: {
                    legend: {
                        position: 'top',
                    },
                    title: {
                        display: false,
                        text: name
                    },
                    decimation: {
                        enabled: false,
                        algorithm: 'min-max',
                    }
                }
            }
        });
    }

    setSuccessCount(successCount) {
        if (successCount > this._successCount[0]) {
            this._successCount[0] = successCount;
        }
    }

    setTimeoutCount(timeoutCount) {
        if (timeoutCount > this._timeoutCount[0]) {
            this._timeoutCount[0] = timeoutCount;
        }
    }

    setErrorCount(errorCount) {
        if (errorCount > this._errorCount[0]) {
            this._errorCount[0] = errorCount;
        }
    }

    get chart() {
        return this._chart;
    }

    get chartId() {
        return this._chartId;
    }

    get name() {
        return this._name;
    }

    reset() {
        this._successCount[0] = 0;
        this._timeoutCount[0] = 0;
        this._errorCount[0] = 0;
        this._chart.reset();
    }
}

export class StateContainer {
    constructor() {
        this._dataCounters = {};
        this._nameToLineChartContainerMap = {};
        this._nameToBarChartContainerMap = {};
    }

    getDataCounter(name) {
        if (!this._dataCounters[name]) {
            this._dataCounters[name] = 0;
        }

        return this._dataCounters[name];
    }

    incrementDataCounter(name) {
        if (!this._dataCounters[name]) {
            this._dataCounters[name] = 1;
        } else {
            this._dataCounters[name]++;
        }
    }

    resetDataCounter(name) {
        this._dataCounters[name] = 0;
    }

    getLineChartContainer(name) {
        return this._nameToLineChartContainerMap[name];
    }

    setLineChartContainer(chartContainer) {
        const name = chartContainer.name;

        return this._nameToLineChartContainerMap[name] = chartContainer;
    }

    getBarChartContainer(name) {
        return this._nameToBarChartContainerMap[name];
    }

    setBarChartContainer(chartContainer) {
        const name = chartContainer.name;

        return this._nameToBarChartContainerMap[name] = chartContainer;
    }

    hardReset() {
        this._dataCounters = {};
        this._nameToLineChartContainerMap = {};
        this._nameToBarChartContainerMap = {};
    }

    reset() {
        for (const name in this._nameToLineChartContainerMap) {
            this._nameToLineChartContainerMap[name].reset()
        }

        for (const name in this._nameToBarChartContainerMap) {
            this._nameToBarChartContainerMap[name].reset()
        }
    }

    isExist(name) {
        return !!this._dataCounters[name];
    }
}
