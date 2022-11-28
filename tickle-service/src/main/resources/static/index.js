'use strict';

import {BarChartContainer, ColorGroup, ColorPack, LineChartContainer, StateContainer, UrlProvider} from "./model.js"
import {renderCanvasesRow, renderHeader, renderHttpMethodSelector, renderPrey, renderRpsSliderOptions} from "./render.js";

const colorPack = new ColorPack(
    new ColorGroup('rgba(0, 255, 0, 1)', 'rgba(0, 255, 0, 0.5)'),
    new ColorGroup('rgba(255, 255, 0, 1)', 'rgba(255, 255, 0, 0.5)'),
    new ColorGroup('rgba(255, 0, 0, 1)', 'rgba(255, 0, 0, 0.5)')
);
const urlProvider = new UrlProvider(window.location);
const stateContainer = new StateContainer();
const headerNameToValueMap = {
    "Content-Type": "application/json",
    "Accept": "*/*",
    "Accept-Encoding": "gzip, deflate, br"
};

await registerSliderForm();
await registerSubmitNewPreyEventListener();
await reloadPreys();
await renderHeaders();
await registerDownloadPreysListener();
renderHttpMethodSelectors();
registerDownloadPreysLink();

async function registerSliderForm() {
    const tickleOptionsBody = await fetch(urlProvider.tickleOptionsUrl, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
    });
    const tickleOptions = await tickleOptionsBody.json()

    stateContainer.rps = tickleOptions.rps;

    renderRpsSliderOptions(0, tickleOptions.rps, rpsSlideOnchangeFunction);

    document.getElementById("loadTimeInputId").value = tickleOptions.loadTimeSec;
    document.getElementById("stopLoadWhenDisconnectInput").checked = tickleOptions.stopWhenDisconnect;

    onSliderStickyContainerEvent();
}

async function rpsSlideOnchangeFunction(that) {
    const additionalSliderOptionsForm = document.getElementById("additionalSliderOptionsForm");
    const stopLoadWhenDisconnectInput = document.getElementById("stopLoadWhenDisconnectInput");
    const loadTimeInputId = document.getElementById("loadTimeInputId");

    if (additionalSliderOptionsForm.checkValidity()) {
        const value = that.value;

        stateContainer.rps = value;

        resetCharts();

        await runTickle(value, stopLoadWhenDisconnectInput.checked, loadTimeInputId.value);
        await reconnectToAllTickles();
    }

    additionalSliderOptionsForm.classList.add('was-validated');
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

async function registerSubmitNewPreyEventListener() {
    const preyConfigForm = document.getElementById("preyConfigForm");

    preyConfigForm.addEventListener("submit", async (event) => {
        const newPreyName = document.getElementById("newPreyName").value;
        const newPreyUrl = document.getElementById("newPreyUrl").value;
        const method = Array.from(document.getElementById("httpMethodSelector").children).find((selector) => selector.checked).value;
        const loadRequestParams = document.getElementById("loadRequestParams").value;
        const loadRequestBody = document.getElementById("loadRequestBody").value;
        const expectedTime = document.getElementById("responseTimeoutInputId").value;
        const expectedResponseStatusCode = document.getElementById("expectedStatusCodeInputId").value;

        event.preventDefault();

        if (preyConfigForm.checkValidity() && !stateContainer.isExist(newPreyName)) {
            await registerPrey(
                newPreyName,
                newPreyUrl,
                method,
                loadRequestParams,
                headerNameToValueMap,
                loadRequestBody,
                expectedTime,
                expectedResponseStatusCode
            );
        } else {
            event.stopPropagation();
        }

        preyConfigForm.classList.add('was-validated')
    });
}

function registerDownloadPreysLink() {
    document.getElementById("downloadPreysBtnId").href = urlProvider.preyFileUrl;
}

async function registerDownloadPreysListener() {
    const input = document.getElementById("uploadPreysInputId");

    document.getElementById("uploadPreysBtnId").addEventListener("click", (e) => {
        if (input) {
            input.click();
        }
    }, false);

    input.addEventListener("change", async () => {
        const file = input.files[0];

        if (file) {
            const formData = new FormData();
            formData.append("tickle_config", file);
            await fetch(urlProvider.preyFileUrl, {method: "POST", body: formData});
            await reloadPreys();
        }
    });

}

async function registerPrey(name, url, method, requestParameters, headers, requestBody, responseTimeout, expectedResponseStatusCode) {
    await fetch(urlProvider.preyUrl, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            name: name,
            path: url,
            method: method,
            requestParameters: requestParameters,
            headers: headers,
            requestBody: requestBody,
            timeoutMs: responseTimeout,
            expectedResponseStatusCode: expectedResponseStatusCode,
            enabled: true
        })
    });
    await reloadPreys();
}

async function reloadPreys() {
    document.getElementById("preyList").innerHTML = "";
    const chartContainer = document.getElementById("chartContainer");
    chartContainer.innerHTML = "";
    stateContainer.hardReset();
    const response = await fetch(urlProvider.preyUrl, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
    });
    const preys = await response.json();

    preys.forEach((prey, index, array) => {
        stateContainer.addPrey(prey);

        const _name = prey.name;

        renderPrey(prey, onDeletePreyAction, onSwitchEnabledPreyAction)

        if (prey.enabled) {
            renderPreyCharts(_name);

            connectToTickle(_name);
        }
    });

}

function connectToTickle(name) {
    connectToTickleWs(name);
    connectToCountdownWs(name);
}

function reconnectToAllTickles() {
    stateContainer.getAllPreys()
        .filter(_prey => !!_prey.enabled)
        .forEach(_prey => reconnectToTickle(_prey.name));
}


function reconnectToTickle(name) {
    stateContainer.getTickleWs(name).close();
    stateContainer.getCountdownWs(name).close();

    connectToTickleWs(name);
    connectToCountdownWs(name);
}

function renderPreyCharts(name) {
    const lineChartId = name + "_line_chart";
    const barChartId = name + "_bar_chart";

    renderCanvasesRow(lineChartId, barChartId, name);
    stateContainer.setLineChartContainer(new LineChartContainer(lineChartId, name));
    stateContainer.setBarChartContainer(new BarChartContainer(barChartId, name, colorPack));
}

function connectToTickleWs(name) {
    const loadServiceWs = new WebSocket(urlProvider.getLoadWsUrl(name), []);
    stateContainer.setNameToTickleWs(name, loadServiceWs);

    loadServiceWs.onmessage = async (event) => {
        const _data = JSON.parse(event.data);

        await appendData(name, _data);
    }

    loadServiceWs.onopen = async (event) => {
        runRequestDataLoop(name);
        console.log(`[${name}] connected to load ws`);
    }

    loadServiceWs.onerror = (event) => {
        console.error(event);
    }

    loadServiceWs.onclose = (event) => {
        stopRequestDataLoops(name);
        console.log(`[${name}] disconnected from load ws`)
    }

    return loadServiceWs;
}

function connectToCountdownWs(name) {
    const countdownServiceWs = new WebSocket(urlProvider.getCountdownWsUrl(name), []);
    stateContainer.setNameToCountdownWs(name, countdownServiceWs);

    countdownServiceWs.onopen = (event) => {
        console.log(`[${name}] connected to countdown ws`);
    }

    countdownServiceWs.onmessage = (event) => {
        const _data = JSON.parse(event.data);

        updateCountdown(name, _data);
    }

    countdownServiceWs.onerror = (event) => {
        console.error(event);
    }

    countdownServiceWs.onclose = (event) => {
        console.log(`[${name}] disconnected from countdown ws`)
    }

    return countdownServiceWs;
}

async function appendData(name, array) {
    stateContainer.setDataRequestForbidden(name, true);

    array.forEach(data => {
        appendLineChartData(name, data);
        appendBarChartData(name, data);
    });

    if (array.length > 0) {
        await updateCharts(name);
    }

    stateContainer.setDataRequestForbidden(name, false);
}

async function updateCharts(name) {
    await stateContainer.getBarChartContainer(name).chart.update();
    await stateContainer.getLineChartContainer(name).chart.update();
}

function updateCountdown(name, countdownTick) {
    const progressbar = document.getElementById(`load-progressbar-${name}`);
    const initialValue = countdownTick["initial"];
    const currentValue = countdownTick["current"];

    progressbar.setAttribute("aria-valuenow", `${initialValue}`);
    progressbar.style.width = `${(initialValue - currentValue) / initialValue * 100}%`;
    progressbar.innerHTML = `${currentValue}`;
}

function appendLineChartData(name, report) {
    const chartContainer = stateContainer.getLineChartContainer(name);

    chartContainer.addResponseTime(report["responseTime"]);
    chartContainer.addAttemptNumber(report["attemptNumber"]);

    const status = report["status"];

    if (status === "SUCCESS") {
        chartContainer.addPointColorPack(colorPack.success);
    } else if (status === "TIMEOUT") {
        chartContainer.addPointColorPack(colorPack.timeout);
    } else {
        chartContainer.addPointColorPack(colorPack.error);
    }
}

function appendBarChartData(name, report) {
    const chartContainer = stateContainer.getBarChartContainer(name);

    chartContainer.setSuccessCount(report["successCount"]);
    chartContainer.setTimeoutCount(report["timeoutCount"]);
    chartContainer.setErrorCount(report["errorCount"]);
}

async function onDeletePreyAction(name) {
    await fetch(urlProvider.getPreyUrl(name), {
        method: "DELETE",
        headers: {
            "Content-Type": "application/json"
        }
    })
    await reloadPreys();
}

async function onSwitchEnabledPreyAction(that, name) {
    await fetch(urlProvider.getPreyUrl(name), {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            enabled: that.checked
        })
    });
    await reloadPreys();
}

function resetCharts() {
    stateContainer.reset();
}

async function runTickle(rps, isStopLoadWhenDisconnect, loadTimeSec) {
    const body = {
        rps: rps,
        stopWhenDisconnect: isStopLoadWhenDisconnect,
        loadTimeSec: loadTimeSec,
    }

    await fetch(urlProvider.tickleUrl, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        body: JSON.stringify(body)
    });
}

async function renderHeaders() {
    const headerList = document.getElementById("headerList");
    headerList.innerHTML = "";

    for (const name in headerNameToValueMap) {
        renderHeader(name, headerNameToValueMap[name], onDeleteHeaderAction);
    }

    await registerAddHeaderButton();
}

async function onDeleteHeaderAction(name) {
    delete headerNameToValueMap[name];
    await renderHeaders();
}

async function registerAddHeaderButton() {
    document.getElementById("addHeaderButton").onclick = async function () {
        headerNameToValueMap[document.getElementById("loadHeaderName").value] = document.getElementById("loadHeaderValue").value;

        await renderHeaders();
    }
}

function renderHttpMethodSelectors() {
    ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS', 'TRACE'].forEach((value, index, array) => renderHttpMethodSelector(value, !index));
}

function runRequestDataLoop(name) {
    stopRequestDataLoops(name);

    const delay = stateContainer.respiteRenderMs;

    let timerId = setTimeout(async function request() {
        const dataRequestForbidden = stateContainer.isDataRequestForbidden(name);

        if (!dataRequestForbidden) {
            const ws = stateContainer.getTickleWs(name);

            if (ws) {
                await ws.send(1);
            }
        }

        timerId = setTimeout(request, delay);
    }, delay);

    stateContainer.setRequestDataLoopId(name, timerId);
}

function stopRequestDataLoops(name) {
    const prevTimerId = stateContainer.getRequestDataLoopId(name);

    if (prevTimerId) {
        clearInterval(prevTimerId);

        console.log("data loop was stopped")
    }
}
