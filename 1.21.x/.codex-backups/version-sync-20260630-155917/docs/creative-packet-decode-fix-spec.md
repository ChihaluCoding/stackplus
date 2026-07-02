# クリエイティブスロット送信デコード修正仕様

## 範囲
- Minecraft 1.21.9 のクリエイティブインベントリ操作で、1000個スタックをサーバーへ送信できるようにする。
- ルート側の共通ソースにも同じ `ItemStack` CODEC 補正を適用する。

## 制約
- 通常アイテムの上限は `StackLimitConfig.STACK_LIMIT` の 1000 個を維持する。
- vanilla で上限 1 個のアイテムは、既存仕様どおり原則スタック不可のまま維持する。
- `set_creative_mode_slot` の受信時に使われる `ItemStack.CODEC` の `count` 範囲だけを拡張し、パケット形式自体は変更しない。

## 受け入れ条件
- 1000個の通常アイテムをクリエイティブスロットに置いても、`serverbound/minecraft:set_creative_mode_slot` のデコード例外で切断されない。
- `ItemStack.CODEC` の `count` 上限が 99 ではなく `StackLimitConfig.STACK_LIMIT` に差し替わる。
- 1.21.9 プロジェクトがビルドできる。

## 対象外
- 1000個を超えるスタック対応。
- 独自ネットワークパケットや設定画面の追加。
