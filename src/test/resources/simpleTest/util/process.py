import json

def generate(c):
    patstr = '''<pattern>
        <id>pat_1{tag}1</id>
        <freshness>
            <type>time</type>
            <value>48000</value>
        </freshness>
        <matcher>
            <type>function</type>
            <functionName>run_with_service_ending</functionName>
            <extraArgumentList>
                <argument>{tag}</argument>
            </extraArgumentList>
        </matcher>
    </pattern>
    \n
    <pattern>
        <id>pat_1{tag}2</id>
        <freshness>
            <type>time</type>
            <value>48000</value>
        </freshness>
        <matcher>
            <type>function</type>
            <functionName>run_with_service_ending</functionName>
            <extraArgumentList>
                <argument>{tag}</argument>
            </extraArgumentList>
        </matcher>
    </pattern>\n'''.format(tag=c)
    return patstr
    


if __name__ == "__main__":
    # with open("tmp.xml", 'w') as fout:
    #     for c in range(ord('A'), ord('Z') + 1):
    #         fout.write(generate(chr(c)))
    #         fout.flush
    rawData = []
    with open('data_5_0-1.txt') as fin:
        rawData = fin.readlines()
    with open('../data.txt', 'w') as fout:
        for lineData in rawData:
            fields = lineData.split(',')
            jsonData = {}
            jsonData['timestamp'] = fields[0]
            fieldsJson = {}
            fieldsJson['taxiId'] = fields[1]
            fieldsJson['longitude'] = fields[2]
            fieldsJson['latitude'] = fields[3]
            fieldsJson['speed'] = fields[4]
            fieldsJson['direction'] = fields[5]
            fieldsJson['status'] = fields[6].strip()
            jsonData['fields'] = fieldsJson
            fout.write(json.dumps(jsonData) + '\n')
            fout.flush()
