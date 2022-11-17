import {ColorGroup, ColorPack, Prey} from "./model.js"
import {preyList} from "./constants.js";

const colorPack = new ColorPack(
    new ColorGroup('rgba(0, 255, 0, 1)', 'rgba(0, 255, 0, 0.5)'),
    new ColorGroup('rgba(255, 255, 0, 1)', 'rgba(255, 255, 0, 0.5)'),
    new ColorGroup('rgba(255, 0, 0, 1)', 'rgba(255, 0, 0, 0.5)')
);

let dataCounters = {};
let preysToLoad = {};
let nameToLoadWsMap = {};
let nameToLineChartMap = {};
let nameToBarChartMap = {};
let headerNameToValueMap = {
    "Content-Type": "application/json",
    "Accept": "*/*",
    "Accept-Encoding": "gzip, deflate, br"
};

await registerSliderForm();
registerSubmitNewPreyEventListener();
await reloadPreys();
registerAddHeaderButtonButton();
renderHeaders();

async function renderPrey(prey) {
    const _prey = Object.assign(new Prey(), prey)
    const _deleteButtonId = "prey_delete_button_" + _prey.name;
    const _accordionId = "prey_accordion_" + _prey.name;
    let _headersList = "";
    for (const _header in _prey.headers) {
        if (_header) {
            _headersList += `<div>${_header}: ${_prey.headers[_header]}</div>`;
        }
    }
    const _listElement = `
                <li id="prey_${_prey.name}" class="container list-group-item">
                    <div class="row">
                        <div class="col-md-1 d-flex align-items-center">
                            <div class="form-check form-switch d-flex align-items-center">
                              <input class="form-check-input" type="checkbox" role="switch" id="enable_prey_${_prey.name}" checked>
                            </div>
                        </div>

                        <div class="accordion accordion-flush col" id="accordionFlushExample">
                            <div class="accordion-item">
                                <div class="accordion-header" id="flush-headingOne">
                                    <button id="prey_name_${_prey.name}" 
                                    class="accordion-button collapsed" 
                                    type="button" 
                                    data-bs-toggle="collapse"
                                    data-bs-target="#${_accordionId}" 
                                    aria-expanded="false" 
                                    aria-controls="flush-collapseOne">${_prey.name}</button>
                                </div>
                                
                                <div id="${_accordionId}" class="accordion-collapse collapse" aria-labelledby="flush-headingOne" data-bs-parent="#accordionFlushExample">
                                    <div id="prey_url_${_prey.name}" class="accordion-body">${_prey.method}: ${_prey.path}${_prey.requestParameters ? '?' + _prey.requestParameters : ''}</div>
                                    
                                    ${_headersList ? `<div class="accordion-body">${_headersList}</div>` : ""}
                                    
                                    ${_prey.requestBody ? `<div class="accordion-body">
                                        <textarea class="form-control" rows="6" disabled>${_prey.requestBody}</textarea>
                                    </div>` : ''}

                                    <div class="row accordion-body">
                                        <div class="col-md-6 d-flex">Expected success response status code is ${_prey.expectedResponseStatusCode}</div>
                                        <div class="col-md-6 d-flex justify-content-end">Response timeout ${_prey.timeoutMs} ms</div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-md-1 d-flex justify-content-end">
                            <button id="${_deleteButtonId}" type="button" class="btn btn-outline-danger">
                                <svg id="svgDash" xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" class="bi bi-dash" viewBox="0 0 16 16">
                                    <path d="M4 8a.5.5 0 0 1 .5-.5h7a.5.5 0 0 1 0 1h-7A.5.5 0 0 1 4 8z"/>
                                </svg>
                            </button>
                        </div>
                    </div>
                </li>
            `;

    preyList.insertAdjacentHTML("beforeend", _listElement);
    document.getElementById(_deleteButtonId).onclick = async () => {
        if (preysToLoad[_prey.name]) {
            await fetch("http://localhost:8088/prey/" + _prey.name, {
                method: "DELETE",
                headers: {
                    "Content-Type": "application/json"
                }
            })
            await reloadPreys();
        }
    }
}

async function registerSliderForm() {
    onSliderStickyContainerEvent();

    const rpsSlide = document.getElementById('rpsSlide');
    const rpsSliderDiv = document.getElementById("sliderAmount");

    rpsSlide.oninput = function () {
        rpsSliderDiv.innerHTML = this.value;
    }

    const stopLoadWhenDisconnectInput = document.getElementById("stopLoadWhenDisconnectInput");
    const loadTimeInputId = document.getElementById("loadTimeInputId");

    rpsSlide.onchange = function () {
        const value = this.value;

        loadServices(value, stopLoadWhenDisconnectInput.checked, loadTimeInputId.value);
    }

    const loadParameters = await fetch("http://localhost:8088/loadParameters", {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
    });
    const loadOptions = await loadParameters.json()

    rpsSlide.value = loadOptions.rps;
    rpsSliderDiv.innerHTML = loadOptions.rps;
    stopLoadWhenDisconnectInput.checked = loadOptions.stopWhenDisconnect;
}

function registerSubmitNewPreyEventListener() {
    const preyConfigForm = document.getElementById("preyConfigForm");
    preyConfigForm.addEventListener("submit", event => {
        const newPreyName = document.getElementById("newPreyName").value;
        const newPreyUrl = document.getElementById("newPreyUrl").value;
        const method = Array.from(document.getElementById("httpMethodSelector").children).find((selector) => selector.checked).value;
        const loadRequestParams = document.getElementById("loadRequestParams").value;
        const loadRequestBody = document.getElementById("loadRequestBody").value;
        const expectedTime = document.getElementById("responseTimeoutInputId").value;
        const expectedResponseStatusCode = document.getElementById("expectedStatusCodeInputId").value;

        if (preyConfigForm.checkValidity()) {
            if (!preysToLoad[newPreyName]) {
                preysToLoad[newPreyName] = newPreyUrl;

                registerPrey(
                    newPreyName,
                    newPreyUrl,
                    method,
                    loadRequestParams,
                    headerNameToValueMap,
                    loadRequestBody,
                    expectedTime,
                    expectedResponseStatusCode
                );
            }
        } else {
            event.stopPropagation();
        }

        preyConfigForm.classList.add('was-validated')
        event.preventDefault();
    });
}

async function reloadPreys() {
    preyList.innerHTML = "";
    const chartContainer = document.getElementById("chartContainer");
    chartContainer.innerHTML = "";
    preysToLoad = {};
    nameToLoadWsMap = {};
    nameToLineChartMap = {};
    const response = await fetch("http://localhost:8088/prey", {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
    });
    const preys = await response.json();

    await preys.forEach((prey, index, array) => {
        const _prey = Object.assign(new Prey(), prey)
        const _name = _prey.name;

        preysToLoad[_name] = _prey.path;
        renderPrey(prey);
        renderPreyCharts(_name);
        connectToLoadWs(_name);
        connectToCountdownWs(_name);
    });
}

async function registerPrey(name, url, method, requestParameters, headers, requestBody, responseTimeout, expectedResponseStatusCode) {
    const body = {
        name: name,
        path: url,
        method: method,
        requestParameters: requestParameters,
        headers: headers,
        requestBody: requestBody,
        timeoutMs: responseTimeout,
        expectedResponseStatusCode: expectedResponseStatusCode
    }

    await fetch("http://localhost:8088/prey", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(body)
    })
    await reloadPreys();
}

function connectToLoadWs(name) {
    const loadServiceWs = new WebSocket("ws://localhost:8088/websocket/load?" + name, []);
    nameToLoadWsMap[name] = loadServiceWs;
    loadServiceWs.onmessage = (event) => {
        appendData(name, JSON.parse(event.data));
    }

    loadServiceWs.onopen = (event) => {
        console.log(`[${name}] connected to load ws`);
    }


    loadServiceWs.onerror = (event) => {
        console.error(event);
    }

    loadServiceWs.onclose = (event) => {
        console.log(`[${name}] disconnected from load ws`)
    }
}

function appendData(name, data) {
    appendLineChartData(nameToLineChartMap[name], data);
    appendBarChartData(nameToBarChartMap[name], data);

    if (!dataCounters[name]) {
        dataCounters[name] = 0;
        runChartUpdate(name, Number.MIN_SAFE_INTEGER);
        console.log(`Chart for [${name}] was waked up`);
    }

    dataCounters[name]++;

}

function connectToCountdownWs(name) {
    const countdownServiceWs = new WebSocket("ws://localhost:8088/websocket/countdown?" + name, []);

    countdownServiceWs.onopen = (event) => {
        console.log(`[${name}] connected to countdown ws`);
    }

    countdownServiceWs.onmessage = (event) => {
        updateCountdown(name, JSON.parse(event.data));
    }

    countdownServiceWs.onerror = (event) => {
        console.error(event);
    }

    countdownServiceWs.onclose = (event) => {
        console.log(`[${name}] disconnected from countdown ws`)
    }
}

function runChartUpdate(name, prevValue) {
    const counter = dataCounters[name];

    setTimeout(() => {
        if (prevValue !== counter) {

            nameToLineChartMap[name]["chart"].update();
            nameToBarChartMap[name]["chart"].update();

            runChartUpdate(name, counter)
        } else {
            dataCounters[name] = 0;

            console.log(`Chart for [${name}] was snooze`);
        }
    }, 300); // no less than 300!
}

function resetCharts() {
    for (const name in nameToLineChartMap) {
        nameToLineChartMap[name]["chart"].clear();
    }
}

function updateCountdown(name, countdownTick) {
    const progressbar = document.getElementById(`load-progressbar-${name}`);
    const initialValue = countdownTick["initial"];
    const currentValue = countdownTick["current"];

    progressbar.setAttribute("aria-valuenow", `${initialValue}`);
    progressbar.style.width = `${(initialValue - currentValue) / initialValue * 100}%`;
    progressbar.innerHTML = `${currentValue}`;
}

function renderPreyCharts(name) {
    const lineChartId = name + "_line_chart";
    const barChartId = name + "_bar_chart";

    renderCanvasesRow(lineChartId, barChartId, name);
    renderLineChart(lineChartId, name);
    renderBarChart(barChartId, name);
}

function renderLineChart(chartId, name) {
    const chartContainer = {};
    const attemptNumber = [];
    const responseTime = [];
    const pointBackgroundColor = [];
    const pointBorderColor = [];
    chartContainer["attemptNumber"] = attemptNumber;
    chartContainer["responseTime"] = responseTime;
    chartContainer["pointBackgroundColor"] = pointBackgroundColor;
    chartContainer["pointBorderColor"] = pointBorderColor;
    chartContainer["chart"] = new Chart(chartId, {
        type: "line",
        data: {
            labels: attemptNumber,
            datasets: [
                {
                    label: 'Response time, ms',
                    fill: false,
                    pointBackgroundColor: pointBackgroundColor,
                    pointBorderColor: pointBorderColor,
                    pointStyle: 'circle',
                    pointRadius: 8,
                    pointHoverRadius: 15,
                    data: responseTime,
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

    nameToLineChartMap[name] = chartContainer;
}

function renderBarChart(chartId, name) {
    const chartContainer = {};
    const successCount = [0];
    const timeoutCount = [0];
    const errorCount = [0];
    chartContainer["successCount"] = successCount;
    chartContainer["timeoutCount"] = timeoutCount;
    chartContainer["errorCount"] = errorCount;
    chartContainer["chart"] = new Chart(chartId, {
        type: "bar",
        data: {
            labels: ['Count'],
            datasets: [
                {
                    label: 'Success',
                    data: successCount,
                    borderColor: colorPack.success.borderColor,
                    backgroundColor: colorPack.success.backgroundColor,
                    borderWidth: 2,
                    borderRadius: 5,
                    borderSkipped: false,
                },
                {
                    label: 'Timeout',
                    data: timeoutCount,
                    borderColor: colorPack.timeout.borderColor,
                    backgroundColor: colorPack.timeout.backgroundColor,
                    borderWidth: 2,
                    borderRadius: 5,
                    borderSkipped: false,
                },
                {
                    label: 'Error',
                    data: errorCount,
                    borderColor: colorPack.error.borderColor,
                    backgroundColor: colorPack.error.backgroundColor,
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
    })

    nameToBarChartMap[name] = chartContainer;
}

function appendLineChartData(chartContainer, report) {
    safePush(chartContainer["responseTime"], report["responseTime"]);
    safePush(chartContainer["attemptNumber"], report["attemptNumber"]);

    const status = report["status"];

    if (status === "SUCCESS") {
        safePush(chartContainer["pointBackgroundColor"], colorPack.success.backgroundColor)
        safePush(chartContainer["pointBorderColor"], colorPack.success.borderColor)
    } else if (status === "TIMEOUT") {
        safePush(chartContainer["pointBackgroundColor"], colorPack.timeout.backgroundColor)
        safePush(chartContainer["pointBorderColor"], colorPack.timeout.borderColor)
    } else {
        safePush(chartContainer["pointBackgroundColor"], colorPack.error.backgroundColor)
        safePush(chartContainer["pointBorderColor"], colorPack.error.borderColor)
    }
}

function appendBarChartData(chartContainer, report) {
    const currentSuccessCount = chartContainer["successCount"][0];
    const newSuccessCount = report["successCount"];
    if (newSuccessCount > currentSuccessCount) {
        chartContainer["successCount"][0] = newSuccessCount;
    }

    const currentTimeoutCount = chartContainer["timeoutCount"][0];
    const newTimeoutCount = report["timeoutCount"];
    if (newTimeoutCount > currentTimeoutCount) {
        chartContainer["timeoutCount"][0] = newTimeoutCount;
    }

    const currentErrorCount = chartContainer["errorCount"][0];
    const newErrorCount = report["errorCount"];
    if (newErrorCount > currentErrorCount) {
        chartContainer["errorCount"][0] = newErrorCount;
    }
}

function loadServices(rps, isStopLoadWhenDisconnect, loadTimeSec) {
    for (const name in preysToLoad) {
        const preyEnabled = document.getElementById("enable_prey_" + name).checked;
        const msg = {
            rps: rps,
            stopWhenDisconnect: isStopLoadWhenDisconnect,
            loadTimeSec: loadTimeSec,
        }

        if (preyEnabled) {
            nameToLoadWsMap[name].send(JSON.stringify(msg));
        }
    }
}

function safePush(arr, element, maxSize = 100) {
    arr.push(element);
}

function renderCanvasesRow(lineChartId, barChartId, name) {
    const canvas = `
                <label class="container-fluid text-center mt-1" for="chart-row-${name}">${name}</label>
                <div id="chart-row-${name}" class="row justify-content-between">

                    <div id="cnavas_${lineChartId}" class="col-md-10 align-items-start">
                        <canvas id="${lineChartId}" width="100%" height="22"></canvas>
                    </div>
                    <div id="cnavas_${barChartId}" class="col-md-2 align-items-start">
                        <canvas id="${barChartId}" width="100%" height="116"></canvas>
                    </div>
                </div>
                <div class="progress mb-4">
                   <div id="load-progressbar-${name}" class="progress-bar progress-bar-striped" role="progressbar" aria-label="Example with label" style="width: 0;" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100"></div>
                </div>
            `;

    document.getElementById("chartContainer").insertAdjacentHTML("beforeend", canvas);
}

function renderHeaders() {
    const headerList = document.getElementById("headerList");
    headerList.innerHTML = "";

    for (const name in headerNameToValueMap) {
        renderHeader(name, headerNameToValueMap[name]);
    }
}

function renderHeader(name, value) {
    const listElementId = "header_" + name;
    const headerNameId = "header_name_" + name;
    const headerValueId = "header_" + value;
    const deleteHeaderButtonId = "delete_header_button_" + name;
    const listElement = `
                <li id="${listElementId}" class="container list-group-item">
                    <div class="row">
                        <div class="col-md-5 align-self-center">
                            <div id="${headerNameId}">${name}</div>
                        </div>

                        <div class="col align-self-center">
                            <div id="${headerValueId}">${value}</div>
                        </div>

                        <div class="col-md-1 d-flex justify-content-end">
                            <button id="${deleteHeaderButtonId}" type="button" class="btn btn-outline-danger">
                                <svg id="svgDash" xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" class="bi bi-dash" viewBox="0 0 16 16">
                                    <path d="M4 8a.5.5 0 0 1 .5-.5h7a.5.5 0 0 1 0 1h-7A.5.5 0 0 1 4 8z"/>
                                </svg>
                            </button>
                        </div>
                    </div>
                </li>
            `;

    document.getElementById("headerList").insertAdjacentHTML("beforeend", listElement);

    registerDeleteHeaderButton(deleteHeaderButtonId, name);
}

function registerAddHeaderButtonButton() {
    document.getElementById("addHeaderButton").onclick = function () {
        headerNameToValueMap[document.getElementById("loadHeaderName").value] = document.getElementById("loadHeaderValue").value;

        renderHeaders();
    }
}

function registerDeleteHeaderButton(id, name) {
    document.getElementById(id).onclick = function () {
        delete headerNameToValueMap[name];
        renderHeaders();
    }
}

function onSliderStickyContainerEvent() {
    const stickyContainer = document.getElementById("sliderStickyContainer");
    const observer = new IntersectionObserver(
        ([e]) => {
            const target = e.target;
            target.querySelector("#stickyButtonContainer").classList.toggle('isSticky', e.intersectionRatio < 1)
            target.querySelector("#stickyTickmarksContainer").classList.toggle('isSticky', e.intersectionRatio < 1)
        },
        {threshold: [1]}
    );

    observer.observe(stickyContainer)
}
