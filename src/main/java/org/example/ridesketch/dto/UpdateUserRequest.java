package org.example.ridesketch.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {

    private String nickname;

    private String avatar;

    private String email;
}
