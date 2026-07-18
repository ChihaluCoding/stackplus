# Issue auto-close

Issue本文に次のHTMLコメントを追加すると、指定日時を過ぎた後にIssueが自動でクローズされます。

```html
<!-- auto-close: 2026-07-20T18:00:00+09:00 -->
```

日時はISO 8601形式で、`+09:00`などのUTCオフセットまたは`Z`を必ず指定してください。Workflowは5分ごとに期限を確認しますが、GitHub Actionsの混雑状況によって実行が遅れる場合があります。

## 手動確認

リポジトリの **Actions** → **Close issues at the specified time** → **Run workflow** から手動実行できます。

## 対象外

- Pull Request
- 日時指定がないIssue
- タイムゾーンがない日時
- ISO 8601として解釈できない日時
