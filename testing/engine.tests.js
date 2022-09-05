const {httpClient} = require("./fixtures");
const {delay} = require("./utils");
const {
    validate: uuidValidate
} = require("uuid");

const TaskStatus = {
    Finished: "Finished",
    Failed: "Failed"
}

describe('POST /tasks', () => {
    describe('happy path', () => {
        let task;
        let taskId;
        let createTaskResponse;

        beforeAll(async () => {
            // Arrange
            task = generateValidTask();

            // Act
            createTaskResponse = await httpClient.post("tasks", task);
        });

        it('should return created task data', async () => {
            // Assert
            expectToBeValidCreateTaskResponse(createTaskResponse);
            taskId = createTaskResponse.data.id;
        });

        it('should eventually return "Finished" task from Api', async () => {
            // Assert
            let task = (await requestTaskFromApi(taskId)).data;
            while (task.status !== TaskStatus.Finished) {
                await delay(300);
                task = (await requestTaskFromApi(taskId)).data;
            }

            expect(task.status).toBe(TaskStatus.Finished);
            expect(task.stdout).toEqual(["2"]);
            expect(task.stderr).toBeFalsy();
            expect(task.exitCode).toBe(0);
        });
    });

    describe.each([
        ["Nnewtonsoft.Json", "13.0.1"],
        ["Newtonsoft.Json", "333.333.333"],
    ])('with non-existing package (name: %s, version: %s)', (name, version) => {
        let task;
        let taskId;
        let createTaskResponse;

        beforeAll(async () => {
            // Arrange
            task = generateValidTask();
            task.nuget_packages = [{"name":name, "version":version}];

            // Act
            createTaskResponse = await httpClient.post("tasks", task);
        });

        it('should return created task data', async () => {
            // Assert
            expectToBeValidCreateTaskResponse(createTaskResponse);
            taskId = createTaskResponse.data.id;
        });

        it('should eventually return "Failed" task from Api', async () => {
            // Assert
            let task = (await requestTaskFromApi(taskId)).data;
            while (task.status !== TaskStatus.Failed) {
                await delay(300);
                task = (await requestTaskFromApi(taskId)).data;
            }

            expect(task.status).toBe(TaskStatus.Failed);
            expect(task.stdout).toBeFalsy();
            expect(task.stderr).toBeFalsy();
            expect(task.error.includes("NU")).toBe(true);
            expect(task.exitCode).toBeFalsy();
        });
    });

    describe('with non-compilable code', () => {
        let task;
        let taskId;
        let createTaskResponse;

        beforeAll(async () => {
            // Arrange
            task = generateValidTask();
            task.code = `
                    Console.WriteLine(int.Parse(args[0]) + int.Parse(args[1]));
                    foo;
                    bar;
                `;

            // Act
            createTaskResponse = await httpClient.post("tasks", task);
        });

        it('should return created task data', async () => {
            // Assert
            expectToBeValidCreateTaskResponse(createTaskResponse);
            taskId = createTaskResponse.data.id;
        });

        it('should eventually return "Failed" task from Api', async () => {
            // Assert
            let task = (await requestTaskFromApi(taskId)).data;
            while (task.status !== TaskStatus.Failed) {
                await delay(300);
                task = (await requestTaskFromApi(taskId)).data;
            }

            expect(task.status).toBe(TaskStatus.Failed);
            expect(task.stdout).toBeFalsy();
            expect(task.stderr).toBeFalsy();
            expect(task.error.includes("CS")).toBe(true);
            expect(task.exitCode).toBeFalsy();
        });
    });
});

function requestTaskFromApi(taskId) {
    return httpClient.get(`tasks/${taskId}`);
}

function generateValidTask() {
    return {
        code: `
                Console.WriteLine(int.Parse(args[0]) + int.Parse(args[1]));
              `,
        arguments: ["1", "1"],
        target_framework_monikier: "net6.0",
        nuget_packages: [{"name":"Newtonsoft.Json","version":"13.0.1"}]
    };
}

function expectToBeValidCreateTaskResponse(createTaskResponse) {
    expect(createTaskResponse).toBeTruthy();
    expect(createTaskResponse.status).toBe(201);
    expect(createTaskResponse.data).toBeTruthy();
    expect(createTaskResponse.data.id).toBeTruthy();
    expect(uuidValidate(createTaskResponse.data.id)).toBe(true);
    expect(createTaskResponse.data.status).toBe("Created");
}