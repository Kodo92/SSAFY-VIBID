package com.vibid.member.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vibid.auth.domain.Authority;
import com.vibid.auth.domain.Role;
import com.vibid.auth.jwt.domain.Jwt;
import com.vibid.auth.jwt.domain.JwtAuthentication;
import com.vibid.auth.jwt.domain.JwtAuthenticationToken;
import com.vibid.board.domain.Board;
import com.vibid.board.domain.Wish;
import lombok.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "member_id")
  private Long id;

  @Embedded
  @Column(unique = true)
  private Email principal;
  private String credential;
  private String nickname;

  @Builder.Default
  private LocalDateTime registDate = LocalDateTime.now();

  @Builder.Default
  private String profileImageUrl = null;

  @Builder.Default
  private int loginCount = 0;

  @Builder.Default
  private int loginFailCount = 0;

  @Builder.Default
  private boolean isEmailAuthentication = false;

  @Builder.Default
  private boolean isLock = false;

  @Builder.Default
  private boolean isWithdrawal = false;

  @Builder.Default
  @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
  @JsonIgnore
  private Set<Authority> authorities = new HashSet<>();

  @Builder.Default
  @OneToMany(mappedBy = "member")
  @JsonIgnore
  private List<Board> boards = new ArrayList<>();

  @Builder.Default
  @OneToMany(mappedBy = "member")
  @JsonIgnore
  private List<Wish> wishes = new ArrayList<>();

  @Builder.Default
  private String vendor = "vibid";

  public Set<Role> getRoles() {
    return authorities.stream()
      .map(memberAuthority -> memberAuthority.getAuthority())
      .collect(Collectors.toSet());
  }

  // ?????? ?????????
  public static Member of(String vendor, Email email, String nickname, String profileImageUrl) {
    return Member.builder()
      .nickname(nickname)
      .principal(email)
      .profileImageUrl(profileImageUrl)
      .vendor(vendor)
      .build();
  }

  // ???????????? ?????????
  public void login(PasswordEncoder passwordEncoder, String password) {
    // TODO ????????? ?????? Custom Exception
    if(!passwordEncoder.matches(password,this.credential)) {
      loginFail();
      throw new IllegalArgumentException("Bad credential");
    }
  }

  public void emailAuthentication() {
    this.isEmailAuthentication = true;
  }

  public void checkLock() {
    // TODO ????????? ?????? Custom Exception
    if(isLock)
      throw new IllegalArgumentException("????????? ???????????????.");
  }

  public void checkEmailAuthentication() {
    if(!isEmailAuthentication)
      throw new IllegalArgumentException("????????? ???????????????.");
  }

  public void withdrawal() {
    this.principal = Email.deletedEmail(String.valueOf(this.id));
    this.nickname = "????????? ??????";
    this.profileImageUrl = null;
    this.isWithdrawal = true;
    this.credential = "";
  }

  public void changePassword(String credential) {
    this.credential = credential;
  }

  public void changeNickname(String nickname) {
    this.nickname = nickname;
  }

  public void changeProfileImageUrl(String url) {
    this.profileImageUrl = url;
  }

  public void loginSuccess() {
    loginCount++;
  }

  public void loginFail() {
    loginFailCount++;
    if(loginFailCount >= 5)
      isLock = true;
  }

  public void resolveLock() {
    isLock = false;
  }

  // Authentication ????????? ?????? ????????? ???????????? ????????? ??????
  public String newApiToken(Jwt jwt, JwtAuthenticationToken authentication) {
    JwtAuthentication principal = (JwtAuthentication) authentication.getPrincipal();
    Jwt.Claims claims = Jwt.Claims.of(
      principal.getId(),
      principal.getNickname(),
      principal.getEmail(),
      getRoleAtAuthorities(authentication).toArray(String[]::new)
    );
    return jwt.newToken(claims);
  }

  public String newApiToken(Jwt jwt, Authentication authentication) {
    Jwt.Claims claims = Jwt.Claims.of(
      id,
      nickname,
      principal,
      getRoleAtAuthorities(authentication).toArray(String[]::new)
    );
    return jwt.newToken(claims);
  }

  private static List<String> getRoleAtAuthorities(Authentication authentication) {
    return authentication.getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .collect(Collectors.toList());
  }

  public Member update(String nickname, String profileImageUrl) {
    this.nickname = nickname;
    this.profileImageUrl = profileImageUrl;
    return this;
  }

  public Set<GrantedAuthority> getGrantedAuthorities() {
    return this.authorities.stream()
      .map(authority -> new SimpleGrantedAuthority(authority.getAuthority().name()))
      .collect(Collectors.toSet());
  }

  public List<Wish> getWishes(){
    return wishes;
  }
}
