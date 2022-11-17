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
    constructor(name, path, method, requestParameters, headers, requestBody, timeoutMs, expectedResponseStatusCode) {
        this.name = name;
        this.path = path;
        this.method = method;
        this.requestParameters = requestParameters;
        this.headers = headers;
        this.requestBody = requestBody;
        this.timeoutMs = timeoutMs;
        this.expectedResponseStatusCode = expectedResponseStatusCode;
    }

}
