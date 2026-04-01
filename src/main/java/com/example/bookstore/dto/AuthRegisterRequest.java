package com.example.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRegisterRequest {

    @NotBlank(message = "Email dang ky khong duoc de trong")
    @Size(max = 100, message = "Email dang ky toi da 100 ky tu")
    @Pattern(
        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$",
        message = "Email dang ky khong dung dinh dang"
    )
    private String username;

    @NotBlank(message = "Mat khau khong duoc de trong")
    @Size(min = 8, max = 72, message = "Mat khau phai tu 8 den 72 ky tu")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\\\d)(?=.*[^A-Za-z\\\\d]).{8,72}$",
        message = "Mat khau phai co chu thuong, chu hoa, so va ky tu dac biet"
    )
    @JsonAlias({"passwordHash", "password"})
    private String password;
}
