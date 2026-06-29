package com.foodmap.user.controller;

import com.foodmap.common.security.CurrentUser;
import com.foodmap.common.security.FoodMapAuthHeaders;
import com.foodmap.user.dto.CurrentUserResponse;
import com.foodmap.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    @Test
    void meAcceptsUserIdHeaderWithoutLegacyAccountIdHeader() throws Exception {
        UserService userService = mock(UserService.class);
        when(userService.currentUser(any(CurrentUser.class))).thenReturn(new CurrentUserResponse(
                1001L,
                null,
                null,
                "小张",
                3001L,
                "NORMAL"
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService)).build();

        mockMvc.perform(get("/api/users/me")
                        .header(FoodMapAuthHeaders.USER_ID, "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.accountId").doesNotExist());

        ArgumentCaptor<CurrentUser> currentUserCaptor = ArgumentCaptor.forClass(CurrentUser.class);
        verify(userService).currentUser(currentUserCaptor.capture());
        assertThat(currentUserCaptor.getValue().userId()).isEqualTo(1001L);
        assertThat(currentUserCaptor.getValue().accountId()).isNull();
    }
}
