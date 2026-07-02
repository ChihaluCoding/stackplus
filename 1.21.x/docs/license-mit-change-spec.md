# MIT ライセンス変更仕様

## 範囲
- プロジェクトのライセンス表記を CC0-1.0 から MIT に変更する。
- `LICENSE` 本文を MIT License に差し替える。
- `fabric.mod.json` の `license` を `MIT` に変更する。
- 配布条件の変更としてパッチバージョンを上げる。

## 制約
- Mod の実行挙動は変更しない。
- Java パッケージ名、Mod ID、依存関係、対応 Minecraft バージョンは変更しない。

## 受け入れ条件
- `LICENSE` が MIT License の本文になる。
- jar 内の `fabric.mod.json` が `"license": "MIT"` と展開される。
- Gradle ビルドが成功する。

## 範囲外
- README や配布ページ本文の最終調整。
