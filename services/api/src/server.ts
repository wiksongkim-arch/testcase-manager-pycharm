import express from 'express';
import cors from 'cors';
import { logger } from './middleware/logger';
import { errorHandler } from './middleware/error-handler';
import { projectsRouter } from './routes/projects';
import { testCasesRouter } from './routes/testcases';
import { gitRouter } from './routes/git';
import { excelRouter } from './routes/excel';

export function createServer(): express.Application {
  const app = express();

  // Middleware
  app.use(cors());
  app.use(express.json({ limit: '10mb' }));
  app.use(logger);

  // Health check
  app.get('/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
  });

  // API Routes
  app.use('/api/projects', projectsRouter);
  app.use('/api/projects', excelRouter);
  app.use('/api/testcases', testCasesRouter);
  app.use('/api/git', gitRouter);

  // Error handler (must be last)
  app.use(errorHandler);

  return app;
}
