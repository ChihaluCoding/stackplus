# StackPlus

StackPlus は、Minecraft のアイテム最大スタック数をまとめて変更できる Fabric 用 mod です。
Mod Menu から 64、999、1K、10K、32K、1M、100M、1B のプリセットを選ぶか、好きな値を直接入力できます。

## Why StackPlus?

- 最大スタック数を `1` から `1,000,000,000` まで設定できます。
- Mod Menu からゲーム内で設定できます。
- `K/M/B` 表示と `99+` 表示を切り替えられます。
- 耐久値を持つアイテムは vanilla 仕様どおりスタック不可のままにします。
- ベッド、ポーション、バケツ、ボート、トーテム、音楽ディスクなど、通常 1 個スタックの一部アイテムにも対応します。
- ホッパーやインベントリ周りのスタック制限にも対応します。
- 日本語と英語に対応しています。

## Recommended Use

普段使いには `999`、`1K`、`10K`、`32K` をおすすめします。
より大きな値を使いたい場合は、`1M`、`100M`、`1B` のプリセットも選べます。

## Supported Version

- Minecraft: `26.2`
- Loader: Fabric
- Optional: Mod Menu

## Configuration

Mod Menu がある場合は、`Mods > StackPlus > Configure` から設定できます。
Mod Menu がない場合は、既定値 `1K` が使用されます。

設定ファイル:

```text
config/stackplus.properties
```

主な設定:

```properties
stackLimit=1000
displayMode=compact
```

`displayMode` は `compact` または `99plus` を指定できます。

## Notes

- 上限を下げても、すでに存在する大きなスタックは自動分割されません。
- 壊れた設定ファイルは `.broken` として退避し、既定値で起動します。
- とても大きなスタック数を使う場合は、他 mod との組み合わせを確認しながら調整してください。

## Build

```bat
gradlew.bat clean build
```

生成物:

```text
build/libs/stackplus-26.2-2.6.0.jar
```

## Publishing Keywords

stack size, bigger stacks, inventory, storage, quality of life, QoL, fabric, mod menu, Minecraft 26.2, stack limit
