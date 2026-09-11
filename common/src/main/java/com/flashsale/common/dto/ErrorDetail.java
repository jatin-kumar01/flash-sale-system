package com.flashsale.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetail {

    private String field;
    private Object rejectedValue;
    private String message;
    private String errorCode;

    public static ErrorDetail of(String field, Object rejectedValue, String message) {
        return ErrorDetail.builder()
                .field(field)
                .rejectedValue(rejectedValue)
                .message(message)
                .build();
    }

    public static ErrorDetail of(String errorCode, String message) {
        return ErrorDetail.builder()
                .errorCode(errorCode)
                .message(message)
                .build();
    }

}




//
//Why use @JsonInclude?
//@JsonInclude(JsonInclude.Include.NON_NULL)
//
//It means:
//
//Any field whose value is null will not be included in the JSON response.
//
//        For example, without it:
//
//        {
//        "field": null,
//        "rejectedValue": null,
//        "message": "Product is out of stock",
//        "errorCode": "STOCK_EXHAUSTED"
//        }
//
//With @JsonInclude(JsonInclude.Include.NON_NULL):
//
//        {
//        "message": "Product is out of stock",
//        "errorCode": "STOCK_EXHAUSTED"
//        }


//  why use this  of()

//Instead of:
//
//  ErrorDetail.builder()
//    .field("quantity")
//    .rejectedValue(-5)
//    .message("Must be greater than 0")
//    .build();
//
//you can simply write:
//
//   ErrorDetail.of(
//    "quantity",
//            -5,
//            "Must be greater than 0"
//);
// 1. First method:
// Use this when a specific FIELD has an invalid value.
//
// Example:
// User enters age = -5
//
// field         = "age"
// rejectedValue = -5
// message       = "Age must be greater than 0"
//
//ErrorDetail.of(
//    "age",
//            -5,
//            "Age must be greater than 0"
//);

// 2. Second method:
// Use this when a BUSINESS/DOMAIN error occurs,
// and there is no specific field to blame.
//
// Example:
// User tries to buy a product,
// but the product is out of stock.
//
// errorCode = "STOCK_EXHAUSTED"
// message   = "Product is out of stock"

//ErrorDetail.of(
//    "STOCK_EXHAUSTED",
//            "Product is out of stock"
//);

// FIELD problem → 3 arguments
//of(field, rejectedValue, message)

// BUSINESS problem → 2 arguments
//of(errorCode, message)