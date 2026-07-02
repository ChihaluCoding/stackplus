# McVer 版別プロジェクト分割仕様

## 範囲
- `McVer` フォルダ内に Minecraft 1.21 系の各正式版専用プロジェクトを作成する。
- 対象バージョンは `1.21`, `1.21.1`, `1.21.2`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`, `1.21.9`, `1.21.10`, `1.21.11` とする。
- 各フォルダは jar だけではなく、`build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/`, `src/`, `README.md`, `LICENSE` を含む独立 Gradle/Fabric プロジェクトにする。
- 各プロジェクトの `minecraft_version`, `yarn_mappings`, `fabric_api_version`, `archives_base_name`, `fabric.mod.json` の Minecraft 依存を対象バージョン専用にする。
- 1.21 系途中の Yarn 名変更に合わせ、各プロジェクトでコンパイルできる描画 Mixin 名へ調整する。

## 制約
- ルートの既存プロジェクトはテンプレートとして使い、不要な機能追加は行わない。
- 有料 API、外部有料サーバー、秘密情報は使用しない。
- `McVer` 内に既存内容がある場合は事前に退避できる状態にする。
- 各版専用プロジェクトなので、`fabric.mod.json` の `minecraft` 依存は `>=1.21 <1.22` ではなく対象バージョン文字列へ固定する。

## 受け入れ条件
- `McVer` 直下に対象 12 バージョン分のフォルダが存在する。
- 各フォルダに Gradle/Fabric プロジェクト一式が存在する。
- 各フォルダの `gradle.properties` が対象 Minecraft 版、Yarn mapping、Fabric API、版別 jar 名を持つ。
- 各フォルダの `src/main/resources/fabric.mod.json` が対象 Minecraft 版だけに依存する。
- 代表版だけでなく、可能な範囲で全版の `gradlew.bat build` を実行し、コンパイル可否を確認する。

## 対象外
- 1.20 系以前および 1.22 系以降のプロジェクト作成。
- 生成 jar の配布作業。
- ゲーム内での長時間動作確認。
