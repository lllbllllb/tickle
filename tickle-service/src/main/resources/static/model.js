export function ColorGroup(borderColor, backgroundColor) {
    this.borderColor = borderColor;
    this.backgroundColor = backgroundColor;
}

export function ColorPack(success, timeout, error) {
    this.success = success;
    this.timeout = timeout;
    this.error = error;
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
