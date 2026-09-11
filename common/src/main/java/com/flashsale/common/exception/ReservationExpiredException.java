package com.flashsale.common.exception;

public class ReservationExpiredException extends BaseCustomException {

    private static final int STATUS_CODE = 410;
    private static final String ERROR_CODE = "RESERVATION_EXPIRED";

    public ReservationExpiredException(String message) {
        super(message, STATUS_CODE, ERROR_CODE);
    }

    public ReservationExpiredException(String reservationId, Long productId) {
        super(String.format("Reservation '%s' for product ID %d has expired and the stock has been released",
                reservationId, productId), STATUS_CODE, ERROR_CODE);
    }
}

/*
* Haan, **`ReservationExpiredException.java` ki need hai**, aur tumhare Flash Sale project mein ye especially useful hai.

Simple Hinglish mein:

### Ye class kya karti hai?

Maan lo user ne product reserve kiya:

```text
Product stock = 1
        ↓
User A ne reserve kiya
        ↓
Reservation valid for 10 minutes
```

Ab User A ne 10 minutes ke andar payment nahi ki.

```text
10 minutes complete
        ↓
Reservation EXPIRED
        ↓
Stock release
        ↓
Stock wapas AVAILABLE
```

Ab agar User A 15 minutes baad payment karne ki koshish kare, system ko **payment allow nahi karni chahiye**.

Yahin `ReservationExpiredException` kaam aati hai.

---

### Without this exception

Agar proper exception nahi hai:

```text
User → Payment
       ↓
Payment succeeds ❌
       ↓
Reservation already expired
       ↓
Stock kisi aur ko mil chuka hai
```

Ye **serious consistency problem** hai.

---

### With this exception

```text
User
 ↓
Payment attempt
 ↓
Check reservation
 ↓
Reservation expired
 ↓
ReservationExpiredException
 ↓
HTTP 410 GONE
 ↓
Payment/order finalization stopped
```

User ko response mil sakta hai:

```json
{
  "errorCode": "RESERVATION_EXPIRED",
  "message": "Reservation has expired and the stock has been released"
}
```

---

### `410 GONE` kyun?

`410 GONE` ka meaning basically:

> Ye resource pehle available tha, lekin ab permanently/currently available nahi hai.

Tumhare case mein:

```text
Reservation
     ↓
Valid
     ↓
TTL expires
     ↓
No longer valid
```

Isliye `410` meaningful hai.

---

### `BaseCustomException` kyun extend kar raha hai?

Tumhare project mein multiple custom errors honge:

```text
BaseCustomException
       │
       ├── ReservationExpiredException
       ├── OutOfStockException
       ├── ProductNotFoundException
       ├── PaymentFailedException
       └── ...
```

Isse **global exception handler** sab errors ko ek consistent format mein handle kar sakta hai.

---

### Is project mein ye important kyun hai?

Flash-sale system mein sabse important cheez hai:

> **Expired reservation ke baad user ko old reservation ke basis par order/payment complete nahi karne dena.**

Flow:

```text
Reserve Product
      ↓
Reservation TTL
      ↓
      ├── Payment in time
      │       ↓
      │      SOLD
      │
      └── TTL expired
              ↓
          Release Stock
              ↓
       ReservationExpiredException
              ↓
       Don't finalize order
```

So **haan, ye file rakhna sahi hai**.

Ek chhoti baat: `410` vs `400` ka decision architecture mein later consistently define karna chahiye. Tumhare current implementation mein `410` specifically chosen hai, jo expired reservation ko clearly distinguish karta hai.

*
* */
