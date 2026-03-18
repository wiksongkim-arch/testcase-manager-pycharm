import { createServer } from './server';

const PORT = process.env.PORT || 3001;

const app = createServer();

app.listen(PORT, () => {
  console.log(`🚀 TestCase Manager API server running on port ${PORT}`);
});
