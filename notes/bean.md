# Spring Bean 教學

## 1. 先從一般 Java 物件開始

假設要建立一個計算機，新增 `Calculator.java`：

```java
package student.ed.gtalent_spring_boot_260801.service;

public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public int multiply(int a, int b) {
        return a * b;
    }
}
```

一般 Java 程式使用時，需要自己建立物件：

```java
Calculator calculator = new Calculator();
System.out.println(calculator.add(10, 20)); // 30
```

如果很多地方都需要 `Calculator`，可以讓 Spring 建立並管理物件，再將它提供給需要的類別。由 Spring 容器管理的物件稱為 **Bean**。

## 2. 使用 `@Bean` 註冊物件

新增 `config/CalculatorConfig.java`：

```java
package student.ed.gtalent_spring_boot_260801.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import student.ed.gtalent_spring_boot_260801.service.Calculator;

@Configuration
public class CalculatorConfig {

    @Bean
    public Calculator calculator() {
        return new Calculator();
    }
}
```

- `@Configuration` 表示這個類別提供 Spring 設定。
- `@Bean` 表示 Spring 要管理這個方法回傳的物件。
- Bean 的類型是 `Calculator`，預設名稱是方法名稱 `calculator`。

在此例中，Spring 會在啟動時呼叫 `calculator()` 並管理回傳的物件。預設 scope 是 **singleton**：同一個 Spring 容器內共用該 Bean，不代表整台電腦只有一個物件。

## 3. 使用建構子注入 Bean

新增 `controller/CalculatorController.java`：

```java
package student.ed.gtalent_spring_boot_260801.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import student.ed.gtalent_spring_boot_260801.service.Calculator;

@RestController
@RequestMapping("/api/demo")
public class CalculatorController {

    private final Calculator calculator;

    public CalculatorController(Calculator calculator) {
        this.calculator = calculator;
    }

    @GetMapping("/add")
    public int add(@RequestParam int a, @RequestParam int b) {
        return calculator.add(a, b);
    }

    @GetMapping("/multiply")
    public int multiply(@RequestParam int a, @RequestParam int b) {
        return calculator.multiply(a, b);
    }
}
```

Spring 建立 Controller 時，看到建構子需要 `Calculator`，就會將已註冊的 Bean 傳入。這個範例只有一個建構子，因此不需要額外加 `@Autowired`。

啟動專案後測試：

```text
GET http://localhost:8080/api/demo/add?a=10&b=20
```

回傳 `30`。另一個端點可測試：

```text
GET http://localhost:8080/api/demo/multiply?a=10&b=20
```

回傳 `200`。

建立與使用流程：

```text
CalculatorConfig 的 @Bean 方法
        ↓ 建立並回傳
Spring 容器管理 Calculator
        ↓ 建構子注入
CalculatorController 呼叫 add / multiply
```

**教學提問：**誰執行 `new Calculator()`？由 `@Bean` 方法中的程式碼建立。誰把它傳給 Controller？Spring 根據建構子的參數類型注入。

## 4. `@Bean` 與 `@Service`

前面的 `Calculator` 也可以改用 `@Service`：

```java
package student.ed.gtalent_spring_boot_260801.service;

import org.springframework.stereotype.Service;

@Service
public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public int multiply(int a, int b) {
        return a * b;
    }
}
```

這個版本讓 Spring 掃描類別並建立 Bean；Controller 不需改變。練習切換時，請移除或停用前面的 `CalculatorConfig`，避免註冊兩個同類型 Bean，使注入產生歧義。

| 方式 | 寫在哪裡 | 適合情況 |
| --- | --- | --- |
| `@Service` | 自己的服務類別 | 一般業務邏輯，由 Spring 掃描並建立 |
| `@Bean` | 設定類別的方法 | 需要自行控制建立方式，或註冊第三方類別 |

## 5. 專案中的實例：PasswordEncoder

本專案的 `SecurityConfig` 使用 `@Bean` 註冊密碼編碼器：

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

`BCryptPasswordEncoder` 是 Spring Security 提供的類別。透過 `@Bean` 建立後，其他類別可以透過建構子注入 `PasswordEncoder`，讓 Spring 提供該物件。

## 6. 回到 PythonController

目前 `PythonController` 負責接收 HTTP 請求，也負責啟動 Python 程式。之後可以將執行 Python 的工作抽到 `PythonService`：

```text
HTTP 請求
    ↓
PythonController：接收參數、處理上傳、回傳結果
    ↓ 建構子注入
PythonService：啟動 Python 並讀取執行結果
```

`PythonService` 可以在類別加 `@Service`；也可以移除 `@Service`，改由設定類別的 `@Bean` 方法建立。通常選一種註冊方式即可。這份文件的程式碼是教學範例，尚未將專案實際重構。

## 7. 練習題

1. 建立 `Calculator`，提供加法與乘法。
2. 使用 `@Configuration` 和 `@Bean` 註冊 `Calculator`。
3. 建立 Controller，透過建構子注入並測試兩個 API。
4. 移除原本的 Bean 設定，改用 `@Service`，比較兩種做法。
5. 嘗試將 `PythonController` 執行 Python 的程式碼抽到 `PythonService`，讓 Controller 專注於處理 HTTP 請求。
