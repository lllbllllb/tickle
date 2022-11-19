export class UrlProvider {
    constructor() {
        this._httpProtocol = "http"
        this._wsProtocol = "ws"
        this._host = "localhost:8088"
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
