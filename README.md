# StackPlus 26.2 専用プロジェクト

Minecraft 26.2 Fabric 専用の StackPlus プロジェクトです。

## ビルド
```bat
gradlew.bat clean build
```

生成物:
- `build/libs/stackplus-26.2-2.0.0.jar`

## 仕様
- ModMenu なし: 最大スタック数は既定値の 1K。
- ModMenu あり: 1 から 1,000,000,000 まで設定可能。
- 耐久値を持つアイテムは vanilla 仕様どおりスタック不可。
