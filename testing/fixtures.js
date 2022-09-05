const axios = require("axios");
const {MongoClient} = require("mongodb");

beforeAll(async () => {
    await mongoClient.connect();
})

afterAll(async () => {
    await mongoClient.close();
});

const mongoClient = new MongoClient(
    process.env.MONGODB_URL ?? 'mongodb://user:password@localhost:27017'
);

const db = mongoClient.db("codeExecutionEngine");

const httpClient = axios.create({
    baseURL: process.env.API_BASE_URL ?? 'http://localhost:8080'
});

httpClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response) {
            return error.response;
        }

        throw error;
    }
)

module.exports = {
    httpClient,
    db
}