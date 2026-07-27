package com.example.aiinterviewassistant.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ApiResponse", description = "统一 HTTP JSON 响应包装。所有业务状态码、消息和业务数据均位于该对象中。")
public class ApiResponse<T> {

    @Schema(description = "业务状态码。200 表示成功；其他值与 HTTP 错误状态对应。", example = "200")
    private Integer code;

    @Schema(description = "面向调用方的非敏感结果说明。", example = "success")
    private String message;

    @Schema(description = "成功时的业务数据；失败时通常为 null。")
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
