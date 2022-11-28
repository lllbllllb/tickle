export async function renderPrey(prey, onPreyDeleteFunction, onPreyEnabledSwitchFunction) {
    const _deleteButtonId = "prey_delete_button_" + prey.name;
    const _accordionId = "prey_accordion_" + prey.name;
    const _enablePreySwitcherId = `enable_prey_${prey.name}`;
    let _headersList = "";
    for (const _header in prey.headers) {
        if (_header) {
            _headersList += `<div>${_header}: ${prey.headers[_header]}</div>`;
        }
    }
    const _listElement = `
                <li id="prey_${prey.name}" class="container list-group-item">
                    <div class="row d-flex align-items-center">
                        <div class="col-md-1 d-flex align-items-center">
                            <div class="form-check form-switch d-flex align-items-center">
                              <input class="form-check-input" type="checkbox" role="switch" id="${_enablePreySwitcherId}" ${prey.enabled ? 'checked' : ''} title="Enable to tickle">
                            </div>
                        </div>

                        <div class="accordion accordion-flush col" id="accordionFlushExample">
                            <div class="accordion-item">
                                <div class="accordion-header" id="flush-headingOne">
                                    <button id="prey_name_${prey.name}" 
                                    class="accordion-button collapsed" 
                                    type="button" 
                                    data-bs-toggle="collapse"
                                    data-bs-target="#${_accordionId}" 
                                    aria-expanded="false" 
                                    aria-controls="flush-collapseOne">${prey.name}</button>
                                </div>
                                
                                <div id="${_accordionId}" class="accordion-collapse collapse" aria-labelledby="flush-headingOne" data-bs-parent="#accordionFlushExample">
                                    <div id="prey_url_${prey.name}" class="accordion-body">${prey.method}: ${prey.path}${prey.requestParameters ? '?' + prey.requestParameters : ''}</div>
                                    
                                    ${_headersList ? `<div class="accordion-body">${_headersList}</div>` : ""}
                                    
                                    ${prey.requestBody ? `<div class="accordion-body">
                                        <textarea class="form-control" rows="6" disabled>${prey.requestBody}</textarea>
                                    </div>` : ''}

                                    <div class="row accordion-body">
                                        <div class="col-md-6 d-flex">Expected success response status code is ${prey.expectedResponseStatusCode}</div>
                                        <div class="col-md-6 d-flex justify-content-end">Response timeout ${prey.timeoutMs} ms</div>
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

    // render
    document.getElementById("preyList").insertAdjacentHTML("beforeend", _listElement);
    // render

    document.getElementById(_deleteButtonId).onclick = async function () {
        await onPreyDeleteFunction(prey.name);
    }

    document.getElementById(_enablePreySwitcherId).onchange = async function () {
        await onPreyEnabledSwitchFunction(this, prey.name);
    }
}

export function renderHeader(name, value, onDeleteHeaderAction) {
    const _listElementId = "header_" + name;
    const _headerNameId = "header_name_" + name;
    const _headerValueId = "header_" + value;
    const _deleteHeaderButtonId = "delete_header_button_" + name;
    const _listElement = `
                <li id="${_listElementId}" class="container list-group-item">
                    <div class="row">
                        <div class="col-md-5 align-self-center">
                            <div id="${_headerNameId}">${name}</div>
                        </div>

                        <div class="col align-self-center">
                            <div id="${_headerValueId}">${value}</div>
                        </div>

                        <div class="col-md-1 d-flex justify-content-end">
                            <button id="${_deleteHeaderButtonId}" type="button" class="btn btn-outline-danger">
                                <svg id="svgDash" xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" class="bi bi-dash" viewBox="0 0 16 16">
                                    <path d="M4 8a.5.5 0 0 1 .5-.5h7a.5.5 0 0 1 0 1h-7A.5.5 0 0 1 4 8z"/>
                                </svg>
                            </button>
                        </div>
                    </div>
                </li>
            `;

    // render
    document.getElementById("headerList").insertAdjacentHTML("beforeend", _listElement);
    // render

    document.getElementById(_deleteHeaderButtonId).onclick = async function () {
        await onDeleteHeaderAction(name);
    }
}

export function renderCanvasesRow(lineChartId, barChartId, name) {
    const canvas = `
                <label class="container-fluid text-center mt-1" for="chart-row-${name}">${name}</label>
                <div id="chart-row-${name}" class="row justify-content-between">

                    <div id="cnavas_${lineChartId}" class="col-md-10 align-items-start px-0">
                        <canvas id="${lineChartId}" width="100%" height="22"></canvas>
                    </div>
                    <div id="cnavas_${barChartId}" class="col-md-2 align-items-start px-0">
                        <canvas id="${barChartId}" width="100%" height="116"></canvas>
                    </div>
                </div>
                <div class="progress mb-4">
                   <div id="load-progressbar-${name}" class="progress-bar progress-bar-striped" role="progressbar" aria-label="Example with label" style="width: 0;" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100"></div>
                </div>
            `;

    // render
    document.getElementById("chartContainer").insertAdjacentHTML("beforeend", canvas);
    // render
}

export function renderHttpMethodSelector(method, checked) {
    const _methodSelector = `
                <input type="radio" class="btn-check" name="btnradio" id="btnradio${method}" autocomplete="off" value="${method}" ${checked ? 'checked' : ''}>
                <label class="btn btn-outline-primary" for="btnradio${method}">${method}</label>
        `;

    // render
    document.getElementById("httpMethodSelector").insertAdjacentHTML("beforeend", _methodSelector);
    // render
}

export function renderRpsSliderOptions(resolution, currentRps, rpsSlideOnchangeFunction, watchLive, stopWhenDisconnect) {
    const _currentRps = Number(currentRps);
    const _resolution = !!resolution ? Number(resolution) : _currentRps === 0 ? 1000 : _currentRps > 1000 ? 10000 : _currentRps > 100 ? 1000 : 100;
    const _step = _resolution === 100 ? 1 : _resolution === 1000 ? 4 : 10;
    const _values = [];

    for (let i = 0.1; i < 1; i += 0.1) {
        _values.push(Math.round(_resolution * i));
    }

    const rpsSliderContainer = document.getElementById("rpsSliderContainer");
    rpsSliderContainer.innerHTML = "";

    const _rpsSlider = `
            <div class="d-flex justify-content-center">
                <label class="form-label matte-background" for="rpsSlide">Load, RPS:&nbsp;</label><span class="form-label" style="font-weight: bold" id="sliderAmount">${currentRps}</span>
            </div>
            
            <input id="rpsSlide" name="rpsSlide" type="range" min="0" max="${_resolution}" step="${_step}" value="${currentRps}" list="tickmarks" class="form-range matte-background"/>
            
            <div class="container-fluid m-0 p-0" id="stickyTickmarksContainer">
                <datalist id="tickmarks" class=" col-md-12">
                    <option id="rsetSliderId" value="0" label="Reset"></option>   
                    <option value="${_values[0]}" label="${_values[0]}"></option>
                    <option value="${_values[1]}" label="${_values[1]}"></option>
                    <option value="${_values[2]}" label="${_values[2]}"></option>
                    <option value="${_values[3]}" label="${_values[3]}"></option>
                    <option value="${_values[4]}" label="${_values[4]}"></option>
                    <option value="${_values[5]}" label="${_values[5]}"></option>
                    <option value="${_values[6]}" label="${_values[6]}"></option>
                    <option value="${_values[7]}" label="${_values[7]}"></option>
                    <option value="${_values[8]}" label="${_values[8]}"></option>
                    <option value="${_resolution}" label="${_resolution}"></option>
                </datalist>
            </div>
    `;

    // render
    rpsSliderContainer.insertAdjacentHTML("beforeend", _rpsSlider);
    // render

    const rpsSlide = document.getElementById('rpsSlide');
    const rpsSliderDiv = document.getElementById("sliderAmount");

    rpsSlide.oninput = function () {
        rpsSliderDiv.innerHTML = this.value;
    }

    rpsSlide.onchange = function () {
        rpsSlideOnchangeFunction(this);
    }

    const additionalSliderOptionsContainer = document.getElementById("additionalSliderOptionsContainer");
    additionalSliderOptionsContainer.innerHTML = "";

    const _resolutionSelector = `
         <div class="row mb-3 d-flex align-items-end" >
            <div class="col">
                <label for="rpsResolutionSelector" class="form-label">RPS resolution</label>
                <div id="rpsResolutionSelector" class="container btn-group col" role="group" aria-label="Http methods group">
                    <input type="radio" class="btn-check" name="btnradio" id="btnradio100" autocomplete="off" value="100" ${_resolution === 100 ? 'checked' : ''}>
                    <label class="btn btn-outline-primary" for="btnradio100">100</label>
                    
                    <input type="radio" class="btn-check" name="btnradio" id="btnradio1000" autocomplete="off" value="1000" ${_resolution === 1000 ? 'checked' : ''}>
                    <label class="btn btn-outline-primary" for="btnradio1000">1000</label>
                    
                    <input type="radio" class="btn-check" name="btnradio" id="btnradio10000" autocomplete="off" value="10000" ${_resolution === 10000 ? 'checked' : ''}>
                    <label class="btn btn-outline-primary" for="btnradio10000">10000</label>
                </div>
            </div>
        </div>
        
        <div class="row mb-3 d-flex align-items-center">
            <div class="col-md-4">
                <label for="loadTimeInputId" class="form-label">Load time, s</label>
                <input type="number" step="1" class="form-control" min="1" id="loadTimeInputId" value="30" required>
                <div class="invalid-feedback">Must be positive integer</div>
            </div>

            <div class="d-flex justify-content-end col-md-4">
                <div class="form-check form-switch">
                    <input class="form-check-input" type="checkbox" role="switch" id="watchLiveInput" ${watchLive ? 'checked' : ''}>
                    <label class="form-check-label" for="watchLiveInput">Watch live</label>
                </div>
            </div>

            <div class="d-flex justify-content-end col-md-4">
                <div class="form-check form-switch">
                    <input class="form-check-input" type="checkbox" role="switch" id="stopLoadWhenDisconnectInput" ${stopWhenDisconnect ? 'checked' : ''}>
                    <label class="form-check-label" for="stopLoadWhenDisconnectInput">Stop load when disconnect</label>
                </div>
            </div>
        </div>
    `;

    // render
    additionalSliderOptionsContainer.insertAdjacentHTML("afterbegin", _resolutionSelector);
    // render

    const rpsResolutionSelector = document.getElementById("rpsResolutionSelector");

    rpsResolutionSelector.onchange = () => {
        const _newResolution = Array.from(rpsResolutionSelector.children).find((selector) => selector.checked).value;

        renderRpsSliderOptions(_newResolution, currentRps, rpsSlideOnchangeFunction, watchLive, stopWhenDisconnect)
    };

}
