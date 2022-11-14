## 运行模式 run

### 用户需要指定

- approach(默认为INFUSE)
- mode(默认为offline)
- 如果mode为offline, 则需要指定输入文件dataFile
- 如果mode为online, 则CCE中的client应该交给用户写，发送数据
- ruleFile
- bfuncFile
- patternFile
- mfuncFile

### offlineStarter

1. buildRulesAndPatterns
    - 

## 测试模式 test

### 用户需要指定

- approach(默认为INFUSE)
- mode固定为offline
- ruleFile
- bfuncFile
- contextPool