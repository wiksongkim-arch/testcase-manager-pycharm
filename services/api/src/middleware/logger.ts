import { Request, Response, NextFunction } from 'express';

export function logger(req: Request, res: Response, next: NextFunction): void {
  const timestamp = new Date().toISOString();
  const { method, path } = req;
  
  console.log(`[${timestamp}] ${method} ${path}`);
  
  const start = Date.now();
  
  res.on('finish', () => {
    const duration = Date.now() - start;
    const { statusCode } = res;
    console.log(`[${timestamp}] ${method} ${path} - ${statusCode} (${duration}ms)`);
  });
  
  next();
}
