# 配布前整理仕様

## 範囲
- Mod ID 定数を `fabric.mod.json` の `stackplus` に合わせる。
- 処理を持たない空の Mixin クラスを削除する。
- 削除した Mixin を mixin 設定から外す。

## 制約
- Java パッケージ名、既存の mixin 設定ファイル名、公開機能の挙動は変更しない。
- README の jar 名整理、対応バージョン説明、実機確認は今回の範囲外とする。

## 受け入れ条件
- `CustomStackLimit.MOD_ID` とクライアント側ロガー名が `stackplus` になる。
- 空の `PlayerInventoryMixin`、`ClientPlayerEntityMixin`、`HandledScreenMixin` がビルド対象から消える。
- Gradle ビルドが成功する。

## 範囲外
- 配布ページ文章の最終調整。
- Minecraft 1.21.x 各バージョンでの追加起動確認。
