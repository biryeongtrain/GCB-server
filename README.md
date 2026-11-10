# GCB-Server

겜캐배 모드 기능을 다른 모드 단타에서도 이용할 수 있게 하는 모드입니다.

겜캐배 패킷 코드를 보고 자체제작한 코드들입니다.

## 사용법

```groovy
repositories {
    maven {
        url "https://repo.biryeong.kim/releases/"
    }
}

dependencies {
    implementation include("kim.biryeong:GCB-server:1.2.1+26.3")
}
```

현재 버전은 `1.2.1+26.3` 입니다. Minecraft 26.3, Java 25, Fabric Loader 0.19.5 이상,
Fabric API 0.161.0+26.3 이상이 필요합니다. packet-tweaker는 별도로 설치하지 않습니다.

Java 25 환경에서 `./gradlew build`로 테스트와 JAR 생성을 실행합니다.
`./gradlew publish`는 Maven settings의 `qf-repo` 인증을 사용해 기존 저장소에 게시합니다.

## GCB-mod 4.0.0 연동

[26.3 업데이트](https://github.com/eiqq/GCB-mod/releases/tag/v4.0.0)에서 meg-client-mod 수신 코드는 제거되고
`gcb:bulk_data`라는 별도의 fp32 채널로 교체됐습니다. 구형 `meg:*` 프로토콜과 호환되는 채널은 아닙니다.
`minecraft:gcb`의 파티클·키입력·시점·버전 패킷은 기존 형식을 유지합니다.

클라이언트의 `VELOCITY` 수신기도 제거됐으므로 `PlayerVelocityS2CPacket.send(player)`와 `/gcb velocity`는
26.3의 바닐라 속도 패킷을 보냅니다. 기존 Java 호출 방식은 유지하며 GCB 없는 클라이언트에도 전달됩니다.

## Polymer Display 자동 묶음 전송

Polymer `0.18.2+26.3`이 설치되어 있으면 Mixin이 `ElementHolder.tick()`과 두 `sendPacket()` 경로를
감싸 기존 모델의 Display 변환을 자동으로 묶습니다. 별도 holder로 바꾸거나 모델 코드를 수정할 필요가 없습니다.
기존 서브클래스도 부모의 공통 경로를 호출하면 적용됩니다. 공통 경로를 완전히 우회하는 자체 전송 구현은 대상이 아닙니다.

`GCBDisplayElementHolder`는 제거했습니다. 1.1.0에서 해당 클래스로 변경했다면 기존 `ElementHolder`로 되돌리면 됩니다.
Polymer는 배포 JAR에 포함되지 않습니다. 설치되지 않은 환경에서는 `@Pseudo` Mixin을 건너뛰고 기본 GCB 기능만 실행합니다.

- `gcb:bulk_data` 채널을 등록한 관전자에게만 가시적인 Display 변환을 묶어 보냅니다.
- 변환의 translation/left rotation/scale/right rotation과 보간 duration을 fp32로 전달합니다.
- 비지원 클라이언트에는 원래 메타데이터 패킷을 보냅니다. 초기 스폰·재추적은 Polymer의 전체 상태 전송을 유지합니다.
- 변환과 함께 보간 시작값 `0`이 전달된 경우만 변환을 묶습니다. 지연 시작, 시작값 단독 갱신,
  아이템·블록·텍스트·밝기 등 다른 메타데이터가 섞인 패킷은 전체를 바닐라로 보냅니다.
- 틱 경계와 일반 패킷 앞에서 묶음을 비워 순서를 지키며, 128개 단위로 나눕니다.
  위치 이동 패킷 등이 중간에 있으면 한 모델도 여러 묶음으로 나뉠 수 있습니다.
- 기존 보간 시간은 자동 변경하지 않습니다. 매 틱 포즈를 갱신할 때 1틱이 출발점이며,
  갱신 주기가 길면 그 주기와 지연을 고려해야 합니다. 묶음 전송 자체가 보간 알고리즘을 바꾸지는 않습니다.

### 마운트 예측과의 관계

GCB 4의 `MountPrediction`은 로컬 플레이어가 탄 **서버 권한 Mob**의 위치 보간을 1틱으로 줄이고
서버 속도로 1틱 선행 표시합니다. 순수 가상 Item/Block/Text Display는 Mob이 아니므로 대상이 아닙니다.
실제 Mob에 모델을 부착한 탈것이라면 서버가 정확한 속도와 정지 상태를 동기화할 때 활용할 수 있지만,
Display 포즈 전송만 추가해서 이 예측이 활성화되지는 않습니다. 게임 규칙을 바꾸는 전역 속도 동기화는 추가하지 않습니다.

### 검증

`./gradlew build`는 JUnit과 격리된 Fabric GameTest를 실행합니다. GameTest는 일반 Polymer holder와 기존 서브클래스,
Display, Fabric 채널 등록과 기록용 연결을 사용해 지원/미지원 클라이언트, 가시성, 지연 보간,
채널 해제, 고속 바닐라 속도 패킷을 검사합니다. 실제 GCB 클라이언트의 프레임 시간·영상·네트워크 지터는
측정하지 않았으므로 더 부드럽다고 단정하지 않습니다.

프로토콜 기준: [GCB-mod 4793823의 BulkReceiver](https://github.com/eiqq/GCB-mod/blob/479382323474dafc5163f4dc089a37cbf18b29e6/src/main/java/com/eiqui/gcbmod/modelengine/BulkReceiver.java),
[MountPrediction](https://github.com/eiqq/GCB-mod/blob/479382323474dafc5163f4dc089a37cbf18b29e6/src/main/java/com/eiqui/gcbmod/playerControl/MountPrediction.java).
