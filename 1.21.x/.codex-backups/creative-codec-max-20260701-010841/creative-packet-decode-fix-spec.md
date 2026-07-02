# クリエイティブスロット送信デコード修正仕様

## 範囲
- Minecraft 1.21 から 1.21.11 までの各 `McVer` プロジェクトで、クリエイティブインベントリ操作時に設定値のスタックをサーバーへ送信できるようにする。
- ルート側の共通ソースにも同じ `ItemStack` CODEC 補正を適用し、1.21.x 汎用版 jar を生成できるようにする。

## 制約
- 通常アイテムの上限は `StackLimitConfig.getStackLimit()` の設定値を使う。
- vanilla で上限 1 個のアイテムは、既存仕様どおり原則スタック不可のまま維持する。
- `set_creative_mode_slot` の受信時に使われる `ItemStack.CODEC` の `count` 範囲だけを拡張し、パケット形式自体は変更しない。
- 汎用版 jar は Minecraft 1.21 系を許可する `fabric.mod.json` の依存条件を使い、個別 `McVer` の成果物とは別名にする。

## 受け入れ条件
- 各対応バージョンで設定値の通常アイテムをクリエイティブスロットに置いても、`serverbound/minecraft:set_creative_mode_slot` のデコード例外で切断されない。
- 各対応バージョンで `ItemStack.CODEC` の `count` 上限が 99 ではなく `StackLimitConfig.getStackLimit()` に差し替わる。
- 各 `McVer` プロジェクトがビルドできる。
- ルートプロジェクトから `stackplus-1.21.x-all` 名の汎用版 jar が生成できる。

## 対象外
- 独自ネットワークパケットの追加。
