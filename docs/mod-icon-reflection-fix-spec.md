# Mod アイコン反映修正仕様

## 範囲
- `fabric.mod.json` で指定している Mod アイコンがランチャーや ModMenu で読み込まれやすい形式になるよう修正する。
- 既存のアイコン絵柄は維持し、PNG のキャンバスを正方形にする。

## 制約
- アイコン指定パスは `assets/stackplus/icon.png` を維持する。
- 追加ライブラリや外部サービスは使わない。
- 既存の Mod 機能には触れない。

## 受け入れ条件
- `src/main/resources/assets/stackplus/icon.png` が正方形 PNG になっている。
- ビルド後の jar に `assets/stackplus/icon.png` が含まれる。
- `gradlew.bat build` が成功する。
