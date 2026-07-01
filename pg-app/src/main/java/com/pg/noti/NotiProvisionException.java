package com.pg.noti;

public class NotiProvisionException extends Exception {

    private final String errorCode;
    private final int httpStatus;

    public NotiProvisionException(String message, String errorCode) {
        this(message, errorCode, 0);
    }

    public NotiProvisionException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "NOTI_ERROR";
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
