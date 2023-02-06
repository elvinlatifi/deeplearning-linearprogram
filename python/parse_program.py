import json

file = open("output_program.json")

data = json.load(file)

print(data)

file.close()